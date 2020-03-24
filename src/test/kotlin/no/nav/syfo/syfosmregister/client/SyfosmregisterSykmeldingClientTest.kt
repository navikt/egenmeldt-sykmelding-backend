package no.nav.syfo.syfosmregister.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class SyfosmregisterSykmeldingClientTest : Spek({

    var block: () -> HttpResponseData = {
        respondError(HttpStatusCode.NotFound)
    }
    fun setResponseData(responseData: HttpResponseData) {
        block = { responseData }
    }
    val httpClient = HttpClient(MockEngine) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        engine {
            addHandler { request ->
                block()
            }
        }
    }

    val syfosmregisterClient = SyfosmregisterSykmeldingClient(httpClient, "base")

    describe("test client OK") {
        it("should get sykmelidng") {
            setResponseData(respond(getTestResponse(), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")))
            runBlocking {
                val sykmeldinger = syfosmregisterClient.getSykmelidnger("token", LocalDate.now(), null)
                sykmeldinger.size shouldEqual 1
            }
        }
    }
})
