package no.nav.syfo.application

import com.auth0.jwk.JwkProvider
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.features.CallId
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import java.util.UUID
import no.nav.syfo.Environment
import no.nav.syfo.VaultSecrets
import no.nav.syfo.application.api.registerNaisApi
import no.nav.syfo.application.api.setupSwaggerDocApi
import no.nav.syfo.arbeidsgivere.api.registrerArbeidsgiverApi
import no.nav.syfo.arbeidsgivere.service.ArbeidsgiverService
import no.nav.syfo.db.Database
import no.nav.syfo.db.VaultCredentialService
import no.nav.syfo.log
import no.nav.syfo.metrics.monitorHttpRequests
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.sykmelding.api.registrerEgenmeldtSykmeldingApi
import no.nav.syfo.sykmelding.errorhandling.setUpSykmeldingExceptionHandler
import no.nav.syfo.sykmelding.service.EgenmeldtSykmeldingService
import no.nav.syfo.sykmelding.service.OppdaterTopicsService
import org.apache.kafka.clients.producer.KafkaProducer

@KtorExperimentalAPI
fun createApplicationEngine(
    env: Environment,
    applicationState: ApplicationState,
    vaultSecrets: VaultSecrets,
    jwkProvider: JwkProvider,
    issuer: String,
    arbeidsgiverService: ArbeidsgiverService,
    kafkaProducerReceivedSykmelding: KafkaProducer<String, ReceivedSykmelding>
): ApplicationEngine =
    embeddedServer(Netty, env.applicationPort) {
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }

        setupAuth(vaultSecrets, jwkProvider, issuer)

        install(CallId) {
            generate { UUID.randomUUID().toString() }
            verify { callId: String -> callId.isNotEmpty() }
            header(HttpHeaders.XCorrelationId)
        }
        install(StatusPages) {
            setUpSykmeldingExceptionHandler()
            exception<Throwable> { cause ->
                call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
                log.error("Caught exception", cause)
            }
        }
        val oppdaterTopicsService = OppdaterTopicsService(kafkaProducerReceivedSykmelding = kafkaProducerReceivedSykmelding, sm2013AutomaticHandlingTopic = env.sm2013AutomaticHandlingTopic)
        val egenmeldtSykmeldingService = EgenmeldtSykmeldingService(oppdaterTopicsService, Database(env, VaultCredentialService()))

        routing {
            registerNaisApi(applicationState)
            if (env.cluster == "dev-fss") {
                setupSwaggerDocApi()
                authenticate {
                    registrerEgenmeldtSykmeldingApi(egenmeldtSykmeldingService)
                    registrerArbeidsgiverApi(arbeidsgiverService)
                }
            }
        }
        intercept(ApplicationCallPipeline.Monitoring, monitorHttpRequests())
    }
