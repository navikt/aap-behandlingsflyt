package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

class PdfGeneratorFake : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())

    override fun start() {
        server.start()
    }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()

        routing {
            post("/api/v1/genpdf/innsikt/vurderinger") {
                call.respondBytes(
                    bytes = FAKE_PDF_BYTES,
                    contentType = ContentType.Application.Pdf,
                    status = HttpStatusCode.OK,
                )
            }
        }
    }

    private companion object {
        val FAKE_PDF_BYTES = "%PDF-1.4 fake test pdf".toByteArray()
    }
}
