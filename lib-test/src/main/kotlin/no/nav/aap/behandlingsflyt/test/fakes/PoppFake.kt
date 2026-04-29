package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.InntektForÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.InntektRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.InntektResponse
import no.nav.aap.behandlingsflyt.test.TestPersonService

class PoppFake(private val fakePersoner: () -> TestPersonService) : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("INNTEKT")
        routing {
            post {
                val req = call.receive<InntektRequest>()
                val person = hentEllerGenererTestPerson(fakePersoner(), req.fnr)

                call.respond(
                    InntektResponse(person.inntekter().map { inntekt ->
                        InntektForÅr(
                            inntektAr = inntekt.år.value,
                            belop = inntekt.beløp.verdi().toLong()
                        )
                    }.toList())
                )
            }
        }
    }
}
