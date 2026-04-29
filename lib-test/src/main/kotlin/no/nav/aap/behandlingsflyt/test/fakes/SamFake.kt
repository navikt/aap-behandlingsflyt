package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.datadeling.sam.HentSamIdResponse
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRequest
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRespons
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordningsmeldingApi

class SamFake : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("SAM")
        routing {
            route("/api/vedtak") {
                route("samordne") {
                    post {
                        call.receive<SamordneVedtakRequest>()
                        call.respond(
                            SamordneVedtakRespons(
                                ventPaaSvar = false
                            )
                        )
                    }
                }
                get {
                    call.respond(
                        HttpStatusCode.OK, listOf(
                            HentSamIdResponse(
                                samordningsmeldinger = listOf(
                                    SamordningsmeldingApi(
                                        samId = 123L
                                    )
                                )
                            )
                        )
                    )
                }
            }
        }
    }
}
