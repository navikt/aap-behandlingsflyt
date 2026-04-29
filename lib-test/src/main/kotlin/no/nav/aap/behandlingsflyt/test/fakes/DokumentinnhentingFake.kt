package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.BestillLegeerklæringDto
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.ForhåndsvisBrevRequest
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.HentStatusLegeerklæring
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.PurringLegeerklæringRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringStatusResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.MeldingStatusType
import java.time.LocalDateTime
import java.util.*

class DokumentinnhentingFake : FakeServer() {
    internal val statuser = mutableListOf<LegeerklæringStatusResponse>()

    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() {
        server.start()
    }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("DOKUMENTINNHENTING")
        routing {
            post("/api/dokumentinnhenting/syfo/bestill") {
                val dto = call.receive<BestillLegeerklæringDto>()
                val dialogmeldingId = UUID.randomUUID()
                statuser.add(
                    LegeerklæringStatusResponse(
                        dialogmeldingId,
                        MeldingStatusType.OK,
                        "",
                        dto.behandlerRef,
                        dto.behandlerNavn,
                        UUID.randomUUID().toString(),
                        dto.saksnummer,
                        LocalDateTime.now(),
                        dto.fritekst
                    )
                )
                call.respond(dialogmeldingId.toString())
            }
            post("/api/dokumentinnhenting/syfo/purring") {
                call.receive<PurringLegeerklæringRequest>()
                val dialogmeldingId = UUID.randomUUID()
                statuser.add(
                    LegeerklæringStatusResponse(
                        dialogmeldingId,
                        MeldingStatusType.OK,
                        "",
                        "behandlerRef",
                        "behandlerNavn",
                        UUID.randomUUID().toString(),
                        "saksnummer",
                        LocalDateTime.now(),
                        "fritekst"
                    )
                )
                call.respond(dialogmeldingId.toString())
            }
        }
        routing {
            get("/api/dokumentinnhenting/syfo/status/{saksnummer}") {
                val req = call.receive<HentStatusLegeerklæring>()
                val filtered = statuser.filter { it.saksnummer == req.saksnummer }
                call.respond(filtered)
            }
        }
        routing {
            post("/api/dokumentinnhenting/syfo/brevpreview") {
                val req = call.receive<ForhåndsvisBrevRequest>()
                val fnr = 12341234123
                val navn = "Ronny Råkjører"

                val brev = """
                    Forespørsel om legeerklæring ved arbeidsuførhet\n
                    Gjelder pasient: $navn., $fnr.\n
                    Nav trenger opplysninger fra deg vedrørende din pasient. Du kan utelate opplysninger som etter din vurdering faller utenfor formålet.\n
                    «Legeerklæring ved arbeidsuførhet» leveres på blankett Nav 08-07.08, og honoreres med takst L40.\n
                    ${req.fritekst}\n
                    Lovhjemmel\n
                    Folketrygdloven § 21-4 andre ledd gir Nav rett til å innhente nødvendige opplysninger. Dette gjelder selv om opplysningene er taushetsbelagte, jf. § 21-4 sjette ledd.\n
                    Pålegget om utlevering av opplysninger kan påklages etter forvaltningsloven § 14.\n
                    Klageadgangen gjelder kun lovligheten i pålegget. Fristen for å klage er tre dager etter at pålegget er mottatt. Klagen kan fremsettes muntlig eller skriftlig.\n
                """.trimIndent()

                call.respond(brev)
            }
        }
    }
}
