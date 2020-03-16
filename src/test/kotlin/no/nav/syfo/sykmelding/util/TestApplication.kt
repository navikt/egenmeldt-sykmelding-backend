package no.nav.syfo.sykmelding.util

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.server.testing.TestApplicationEngine
import java.nio.file.Paths
import no.nav.syfo.Environment
import no.nav.syfo.VaultSecrets
import no.nav.syfo.application.setupAuth
import no.nav.syfo.log
import no.nav.syfo.sykmelding.errorhandling.setUpSykmeldingExceptionHandler

fun TestApplicationEngine.setUpTestApplication() {
    start(true)
    application.install(StatusPages) {
        exception<Throwable> { cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
            log.error("Caught exception", cause)
        }
        setUpSykmeldingExceptionHandler()
    }
    application.install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
}

fun TestApplicationEngine.setUpAuth(): Environment {
    val env = Environment(
            kafkaBootstrapServers = "", cluster = "CLUSTERNAME", mqHostname = "", mqPort = 1111, mqChannelName = "", mqGatewayName = "", syfoserviceQueueName = "")

    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    val jwkProvider = JwkProviderBuilder(uri).build()
    val vaultSecrets = VaultSecrets("", "", "1", "", "loginservice", "", "")

    application.setupAuth(vaultSecrets, jwkProvider, "issuer")
    return env
}
