package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreSøknadRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreSøknadResponse
import no.nav.aap.behandlingsflyt.integrasjon.ufore.UføreHistorikkRespons
import no.nav.aap.behandlingsflyt.integrasjon.ufore.UførePeriode
import no.nav.aap.behandlingsflyt.integrasjon.ufore.UføreRequest
import no.nav.aap.behandlingsflyt.test.TestPersonService

class PesysFake(private val fakePersoner: () -> TestPersonService) : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("PESYS")
        routing {
            post("/api/uforetrygd/uforehistorikk/perioder") {
                val body = call.receive<UføreRequest>()
                val hentPerson = fakePersoner().hentPerson(body.fnr)
                if (hentPerson == null) {
                    call.respond(HttpStatusCode.NotFound, "Fant ikke person med fnr ${body.fnr}")
                    return@post
                }
                val uføregrad = hentPerson.uføre
                if (uføregrad == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(
                        HttpStatusCode.OK, UføreHistorikkRespons(
                            uforeperioder = listOf(
                                UførePeriode(
                                    uforegrad = uføregrad.uføregrad.prosentverdi(),
                                    uforegradTom = null,
                                    uforegradFom = uføregrad.virkningstidspunkt,
                                    uforetidspunkt = null,
                                    virkningstidspunkt = uføregrad.virkningstidspunkt
                                )
                            )
                        )
                    )
                }
            }
            post("/api/uforetrygd/ekstern/soknad") {
                val body = call.receive<UføreSøknadRequest>()
                val hentPerson = fakePersoner().hentPerson(body.pid)
                if (hentPerson == null) {
                    call.respond(HttpStatusCode.NotFound, "Fant ikke person med fnr ${body.pid}")
                    return@post
                }
                call.respond(HttpStatusCode.OK, UføreSøknadResponse(hentPerson.uføreSøknad))
            }
        }
    }
}
