package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import java.net.URL
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.application.getWellKnown
import no.nav.syfo.arbeidsgivere.integration.arbeidsforhold.client.ArbeidsforholdClient
import no.nav.syfo.arbeidsgivere.integration.organisasjon.client.OrganisasjonsinfoClient
import no.nav.syfo.arbeidsgivere.service.ArbeidsgiverService
import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.db.Database
import no.nav.syfo.db.VaultCredentialService
import no.nav.syfo.model.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.status.service.StatusendringService
import no.nav.syfo.syfosmregister.client.SyfosmregisterSykmeldingClient
import no.nav.syfo.sykmelding.service.EgenmeldtSykmeldingService
import no.nav.syfo.sykmelding.service.OppdaterTopicsService
import no.nav.syfo.sykmelding.util.KafkaClients
import no.nav.syfo.vault.RenewVaultService
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.egenmeldt-sykmelding-backend")

@KtorExperimentalAPI
fun main() {
    val env = Environment()
    val vaultSecrets = VaultSecrets()

    val wellKnown = getWellKnown(vaultSecrets.oidcWellKnownUri)

    val jwkProvider = JwkProviderBuilder(URL(wellKnown.jwks_uri))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    val kafkaClients = KafkaClients(env, vaultSecrets)
    val applicationState = ApplicationState()

    DefaultExports.initialize()
    val stsOidcClient = StsOidcClient(vaultSecrets.serviceuserUsername, vaultSecrets.serviceuserPassword)
    val httpClient = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
    }

    val arbeidsforholdClient = ArbeidsforholdClient(httpClient, env.registerBasePath)
    val organisasjonsinfoClient = OrganisasjonsinfoClient(httpClient, env.registerBasePath)

    val arbeidsgiverService = ArbeidsgiverService(arbeidsforholdClient, organisasjonsinfoClient, stsOidcClient)

    val oppdaterTopicsService = OppdaterTopicsService(
            kafkaProducerReceivedSykmelding = kafkaClients.kafkaProducerReceivedSykmelding,
            sm2013AutomaticHandlingTopic = env.sm2013AutomaticHandlingTopic)

    val pdlClient = PdlClient(httpClient,
            env.pdlGraphqlPath,
            PdlClient::class.java.getResource("/graphql/getPerson.graphql").readText().replace(Regex("[\n\t]"), ""))

    val pdlService = PdlPersonService(pdlClient, stsOidcClient)
    val syfosmregisterSykmeldingClient = SyfosmregisterSykmeldingClient(httpClient, env.syfosmregisterUrl)
    val syfoserviceKafkaProducer = kafkaClients.syfoserviceKafkaProducer
    val kafkaStatusConsumer = kafkaClients.kafkaStatusConsumer
    val vaultCredentialService = VaultCredentialService()
    val database = Database(env, vaultCredentialService)

    val statusendringService = StatusendringService(database)

    val egenmeldtSykmeldingService = EgenmeldtSykmeldingService(
            oppdaterTopicsService,
            database,
            pdlService,
            syfoserviceKafkaProducer,
            syfosmregisterSykmeldingClient)

    val applicationEngine = createApplicationEngine(
            env,
            applicationState,
            vaultSecrets,
            jwkProvider,
            wellKnown.issuer,
            arbeidsgiverService,
            egenmeldtSykmeldingService
    )

    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()
    applicationState.ready = true

    RenewVaultService(vaultCredentialService, applicationState).startRenewTasks()
    launchListeners(
        applicationState,
        kafkaStatusConsumer,
        statusendringService
    )
}

fun createListener(action: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch {
        try {
            action()
        } catch (ex: Exception) {
            log.error("Noe gikk galt", ex.cause)
            throw ex
        }
    }

@KtorExperimentalAPI
fun launchListeners(
    applicationState: ApplicationState,
    kafkaStatusConsumer: KafkaConsumer<String, SykmeldingStatusKafkaMessageDTO>,
    statusendringService: StatusendringService
) {
    createListener() {
        handleStatusendring(applicationState, kafkaStatusConsumer, statusendringService)
    }
}

suspend fun handleStatusendring(
    applicationState: ApplicationState,
    kafkaStatusConsumer: KafkaConsumer<String, SykmeldingStatusKafkaMessageDTO>,
    statusendringService: StatusendringService
) {
    while (applicationState.ready) {
        kafkaStatusConsumer.poll(Duration.ofMillis(0)).forEach {
            val sykmeldingStatusKafkaMessageDTO: SykmeldingStatusKafkaMessageDTO = it.value()
            try {
                statusendringService.handterStatusendring(sykmeldingStatusKafkaMessageDTO)
            } catch (e: Exception) {
                log.error("Noe gikk galt ved behandling av statusendring for sykmelding med id {}", sykmeldingStatusKafkaMessageDTO.kafkaMetadata.sykmeldingId)
                throw e
            }
        }
        delay(100)
    }
}
