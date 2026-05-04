package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class PdfgenFake : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())

    override fun start() {
        server.start()
    }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()

        routing {
            post("/api/v1/genpdf/aap-saksbehandling-pdfgen/meldekort") {
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
