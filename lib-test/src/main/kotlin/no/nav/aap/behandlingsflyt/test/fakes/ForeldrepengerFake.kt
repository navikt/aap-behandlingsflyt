package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Anvist
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Utbetalingsgrad
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelser
import no.nav.aap.behandlingsflyt.test.TestPersonService
import java.time.LocalDate

class ForeldrepengerFake(private val fakePersoner: () -> TestPersonService) : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() {
        server.start()
    }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("FORELDREPENGER")
        routing {
            post("/hent-ytelse-vedtak") {
                val req = call.receive<ForeldrepengerRequest>()
                val ident = req.ident.verdi
                val fakePerson = fakePersoner().hentPerson(ident)
                if (fakePerson?.foreldrepenger != null) {
                    val foreldrepenger = fakePerson.foreldrepenger

                    // TODO:  utvid til å støtte andre ytelser
                    val response = ForeldrepengerResponse(
                        ytelser = listOf(
                            Ytelse(
                                ytelse = Ytelser.FORELDREPENGER,
                                saksnummer = "352017890",
                                kildesystem = "FPSAK",
                                ytelseStatus = "UNDER_BEHANDLING",
                                vedtattTidspunkt = LocalDate.now().minusMonths(2),
                                anvist = foreldrepenger.map {
                                    Anvist(
                                        periode = it.periode,
                                        utbetalingsgrad = Utbetalingsgrad(it.grad),
                                        beløp = null,
                                    )
                                }
                            )
                        ))

                    call.respond(response.ytelser)
                } else {
                    call.respond(emptyList<Ytelse>())
                }
            }
        }
    }
}
