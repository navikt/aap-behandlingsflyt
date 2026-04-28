package no.nav.aap.behandlingsflyt.test.fakes

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.UtbetaltePerioder
import no.nav.aap.behandlingsflyt.test.TestPersonService

data class SykepengerRequest(val personidentifikatorer: Set<String>)

class SykepengerFake(private val fakePersoner: () -> TestPersonService) : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() {
        server.start()
    }

    private fun module(): Application.() -> Unit = {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            }
        }
        installerStatusPages("SYKEPENGER")
        routing {
            post("/utbetalte-perioder-aap") {
                val request = call.receive<SykepengerRequest>()
                val fakePerson = fakePersoner().hentPerson(request.personidentifikatorer.first())
                if (fakePerson?.sykepenger != null) {
                    call.respond(
                        SykepengerResponse(
                            utbetaltePerioder = fakePerson.sykepenger().map {
                                UtbetaltePerioder(
                                    fom = it.periode.fom,
                                    tom = it.periode.tom,
                                    grad = it.grad,
                                    organisasjonsnummer = null
                                )
                            }
                        ))
                } else {
                    call.respond(SykepengerResponse(emptyList()))
                }
            }
        }
    }
}
