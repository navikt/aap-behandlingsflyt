package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlingsflytToDokumentInnhentingBestillingDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto.MeldingStatusType
import no.nav.aap.dokumentinnhenting.kontrakt.ForhåndsvisDialogmeldingDto
import no.nav.aap.dokumentinnhenting.kontrakt.LegeerklæringPurringDto
import java.time.LocalDateTime
import java.util.*

class DokumentinnhentingFake : FakeServer() {
    internal val statuser = mutableListOf<DialogmeldingStatusTilBehandslingsflytDto>()

    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() {
        server.start()
    }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("DOKUMENTINNHENTING")
        routing {
            post("/api/dokumentinnhenting/syfo/dialogmeldingbestilling") {
                val dto = call.receive<BehandlingsflytToDokumentInnhentingBestillingDto>()
                val dialogmeldingId = UUID.randomUUID()
                statuser.add(
                    DialogmeldingStatusTilBehandslingsflytDto(
                        dialogmeldingUuid = dialogmeldingId,
                        status = MeldingStatusType.OK,
                        statusTekst = "",
                        behandlerRef = dto.behandlerRef,
                        behandlerNavn = dto.behandlerNavn,
                        personId = UUID.randomUUID().toString(),
                        saksnummer = dto.saksnummer,
                        opprettet = LocalDateTime.now(),
                        behandlingsReferanse = dto.behandlingsReferanse,
                        fritekst = dto.dialogmeldingTekst
                    )
                )
                call.respond(dialogmeldingId.toString())
            }
            post("/api/dokumentinnhenting/syfo/purring") {
                val request = call.receive<LegeerklæringPurringDto>()
                val status = requireNotNull(statuser.find { it.dialogmeldingUuid == request.dialogmeldingUuid })
                    .copy(
                        dialogmeldingUuid = UUID.randomUUID(),
                    )
                statuser.add(status)
                call.respond(status.dialogmeldingUuid.toString())
            }
            get("/api/dokumentinnhenting/syfo/status/{saksnummer}") {
                val saksnummer = call.parameters["saksnummer"] ?: error("Mangler saksnummer")
                val filtered = statuser.filter { it.saksnummer == saksnummer }
                call.respond(filtered)
            }
            post("/api/dokumentinnhenting/syfo/brevpreview") {
                val req = call.receive<ForhåndsvisDialogmeldingDto>()
                val fnr = 12341234123
                val navn = "Ronny Råkjører"

                val brev = """
                    Forespørsel om legeerklæring ved arbeidsuførhet\n
                    Gjelder pasient: $navn., $fnr.\n
                    Nav trenger opplysninger fra deg vedrørende din pasient. Du kan utelate opplysninger som etter din vurdering faller utenfor formålet.\n
                    «Legeerklæring ved arbeidsuførhet» leveres på blankett Nav 08-07.08, og honoreres med takst L40.\n
                    ${req.dialogmeldingTekst}\n
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
