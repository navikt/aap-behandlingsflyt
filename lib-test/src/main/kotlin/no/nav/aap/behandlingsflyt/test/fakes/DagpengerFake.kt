package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.integrasjon.samordning.DagpengerPeriodeResponse
import no.nav.aap.behandlingsflyt.integrasjon.samordning.DagpengerRequest
import no.nav.aap.behandlingsflyt.integrasjon.samordning.DagpengerResponse
import no.nav.aap.behandlingsflyt.test.TestPersonService
import org.slf4j.LoggerFactory

class DagpengerFake(private val fakePersoner: () -> TestPersonService) : FakeServer() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("DAGPENGER")
        routing {
            route("/dagpenger/datadeling/v1/perioder") {
                post {
                    val body = call.receive<DagpengerRequest>()
                    val hentPerson = fakePersoner().hentPerson(body.personIdent)
                    val dagpenger = hentPerson?.dagpenger
                    if (hentPerson != null && dagpenger != null) {
                        call.respond(
                            DagpengerResponse(
                                personIdent = body.personIdent,
                                perioder = dagpenger.map { dp ->
                                    DagpengerPeriodeResponse(
                                        fraOgMedDato = dp.periode.fom,
                                        tilOgMedDato = dp.periode.tom,
                                        kilde = dp.kilde,
                                        ytelseType = dp.dagpengerYtelseType
                                    )
                                }.toList()
                            )
                        )
                        return@post
                    }

                    call.respond(
                        DagpengerResponse(
                            perioder = emptyList(),
                            personIdent = body.personIdent
                        )
                    )
                }
            }
        }
    }
}
