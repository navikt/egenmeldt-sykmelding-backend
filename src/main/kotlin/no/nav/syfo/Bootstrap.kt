package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
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
import java.util.concurrent.TimeUnit
import javax.jms.Session
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
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.sykmelding.integration.aktor.client.AktoerIdClient
import no.nav.syfo.sykmelding.service.EgenmeldtSykmeldingService
import no.nav.syfo.sykmelding.service.OppdaterTopicsService
import no.nav.syfo.mq.connectionFactory
import no.nav.syfo.mq.producerForQueue
import no.nav.syfo.sykmelding.util.KafkaClients
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.egenmeldt-sykmelding-backend")

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

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

    val connection = connectionFactory(env).createConnection(vaultSecrets.mqUsername, vaultSecrets.mqPassword)
    connection.start()
    val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    val syfoserviceProducer = session.producerForQueue(env.syfoserviceQueueName)

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
            PdlClient::class.java.getResource("/graphql/getPerson.graphql").readText())

    val pdlService = PdlPersonService(pdlClient, stsOidcClient)

    val egenmeldtSykmeldingService = EgenmeldtSykmeldingService(
            oppdaterTopicsService,
            AktoerIdClient(env.aktoerregisterV1Url, StsOidcClient(vaultSecrets.serviceuserUsername, vaultSecrets.serviceuserPassword), httpClient, vaultSecrets.serviceuserUsername),
            Database(env, VaultCredentialService()),
            pdlService)

    val applicationEngine = createApplicationEngine(
            env,
            applicationState,
            vaultSecrets,
            jwkProvider,
            wellKnown.issuer,
            arbeidsgiverService,
            egenmeldtSykmeldingService,
            session,
            syfoserviceProducer
    )

    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()
    applicationState.ready = true
}
