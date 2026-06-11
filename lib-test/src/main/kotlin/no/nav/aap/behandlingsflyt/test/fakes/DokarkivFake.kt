package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokumentInfo
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Journalpost
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.JournalpostResponse
import no.nav.aap.behandlingsflyt.test.Kjønn.Companion.random
import org.slf4j.LoggerFactory
import kotlin.random.Random

class DokarkivFake : FakeServer() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }


    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        routing {
            route("/rest/journalpostapi/v1/journalpost") {
                post {
                    val journalpost = call.receive<Journalpost>()
                    call.respond(
                        JournalpostResponse(
                            journalpostId = Random.nextLong(1L, Long.MAX_VALUE),
                            melding = null,
                            journalpostferdigstilt = call.request.queryParameters["forsoekFerdigstill"]?.toBoolean() ?: false,
                            dokumenter = journalpost.dokumenter?.mapIndexed { index, _ ->
                                DokumentInfo(dokumentInfoId = (index + 1).toLong())
                            } ?: emptyList()
                        )
                    )
                }
            }
        }
    }
}