package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerYtelseType
import no.nav.aap.behandlingsflyt.integrasjon.samordning.TiltakspengerPeriodeResponse
import no.nav.aap.behandlingsflyt.integrasjon.samordning.TiltakspengerRequest
import no.nav.aap.behandlingsflyt.integrasjon.samordning.TiltakspengerVedtakResponse
import no.nav.aap.behandlingsflyt.test.TestPersonService

class TiltakspengerFake(private val fakePersoner: () -> TestPersonService) : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("TILTAKSPENGER")
        routing {
            route("/vedtak/perioder") {
                post {
                    val body = call.receive<TiltakspengerRequest>()
                    val hentPerson = fakePersoner().hentPerson(body.ident)
                    val tiltakspenger = hentPerson?.tiltakspenger
                    if (hentPerson != null && tiltakspenger != null) {
                        call.respond(tiltakspenger.map { tp ->
                            TiltakspengerVedtakResponse(
                                periode = TiltakspengerPeriodeResponse(
                                    fraOgMed = tp.periode.fom,
                                    tilOgMed = tp.periode.tom
                                ),
                                kilde = tp.kilde,
                                rettighet = TiltakspengerYtelseType.TILTAKSPENGER
                            )
                        })
                        return@post
                    }

                    call.respond(emptyList<TiltakspengerVedtakResponse>())
                }
            }
        }
    }
}
