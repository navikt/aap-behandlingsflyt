package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.test.TestPersonService
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlingsflytToDokumentInnhentingBestillingDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingForhåndsvisningDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto.MeldingStatusType
import no.nav.aap.dokumentinnhenting.kontrakt.FastlegeDto
import no.nav.aap.dokumentinnhenting.kontrakt.ForhåndsvisDialogmeldingDto
import no.nav.aap.dokumentinnhenting.kontrakt.HentFastlegeDto
import no.nav.aap.dokumentinnhenting.kontrakt.LegeerklæringPurringDto
import java.time.LocalDateTime
import java.util.*

class DokumentinnhentingFake(private val fakePersoner: () -> TestPersonService) : FakeServer() {
    internal val statuser =
        mutableListOf<Pair<BehandlingsflytToDokumentInnhentingBestillingDto, DialogmeldingStatusTilBehandslingsflytDto>>()

    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() {
        server.start()
    }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("DOKUMENTINNHENTING")
        routing {
            post("/syfo/dialogmeldingbestilling") {
                val dto = call.receive<BehandlingsflytToDokumentInnhentingBestillingDto>()
                val dialogmeldingId = UUID.randomUUID()
                statuser.add(
                    dto to DialogmeldingStatusTilBehandslingsflytDto(
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
                call.respond(dialogmeldingId)
            }
            post("/syfo/purring") {
                val request = call.receive<LegeerklæringPurringDto>()
                val purrerPå =
                    requireNotNull(statuser.find { it.second.dialogmeldingUuid == request.dialogmeldingUuid })
                val status = purrerPå.copy(
                    second = purrerPå.second.copy(dialogmeldingUuid = UUID.randomUUID()),
                )
                statuser.add(status)
                call.respond(status.second.dialogmeldingUuid.toString())
            }
            get("/syfo/status/{saksnummer}") {
                val saksnummer = call.parameters["saksnummer"] ?: error("Mangler saksnummer")
                val filtered = statuser.filter { it.second.saksnummer == saksnummer }
                call.respond(filtered)
            }
            post("/syfo/brevpreview") {
                val req = call.receive<ForhåndsvisDialogmeldingDto>()

                val brev = """
                    |Standardtekst.
                    |
                    |${req.dialogmeldingTekst}
                    |
                    |Standardtekst.
                    |
                    |Request data:
                    |personNavn = ${req.personNavn}
                    |personIdent = ${req.personIdent}
                    |bestillerNavIdent = ${req.bestillerNavIdent}
                    |dialogmeldingTekst = ${req.dialogmeldingTekst}
                    |dokumentasjonType = ${req.dokumentasjonType}
                    |tidligereBestillingReferanse = ${req.tidligereBestillingReferanse}
                """.trimMargin()

                call.respond(DialogmeldingForhåndsvisningDto(brev))
            }
            post("/syfo/behandleroppslag/fastlege") {
                val request = call.receive<HentFastlegeDto>()
                val person = fakePersoner().hentPerson(request.personIdent)
                call.respond(FastlegeDto(fastlege = person?.fastlege))
            }
        }
    }
}
