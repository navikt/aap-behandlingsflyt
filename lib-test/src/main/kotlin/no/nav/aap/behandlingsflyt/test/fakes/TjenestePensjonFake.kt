package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonRespons
import no.nav.aap.behandlingsflyt.test.TestPersonService

class TjenestePensjonFake(private val fakePersoner: () -> TestPersonService) : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("TP")
        routing {
            get("/api/tjenestepensjon/getActiveForholdMedActiveYtelser") {
                val ident = call.request.headers["fnr"] ?: ""
                val fakePerson = fakePersoner().hentPerson(ident)

                if (fakePerson != null && fakePerson.tjenestePensjon != null) {
                    call.respond(fakePerson.tjenestePensjon)
                } else {
                    call.respond(TjenestePensjonRespons(ident))
                }
            }
        }
    }
}
