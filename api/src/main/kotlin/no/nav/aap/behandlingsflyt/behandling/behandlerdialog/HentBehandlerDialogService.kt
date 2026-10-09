package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumentinnhentingGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.dokumentinnhenting.kontrakt.BegrensetJournalpostDto
import no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType
import no.nav.aap.dokumentinnhenting.kontrakt.FellesDialogmeldingDto
import no.nav.aap.dokumentinnhenting.kontrakt.HentDialogmeldingerForSakParams
import no.nav.aap.dokumentinnhenting.kontrakt.HentDokumentoversiktJournalpostListeParams
import no.nav.aap.dokumentinnhenting.kontrakt.HentLegeerklæringForespørslerForSakParams
import no.nav.aap.dokumentinnhenting.kontrakt.MeldingStatusDto
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.miljo.Miljø
import java.util.*


val DAGER_TIL_PÅMINNELSE = if (Miljø.erProd()) 22L else 1L

class HentBehandlerDialogService(
    private val dokumentinnhentingGateway: DokumentinnhentingGateway,
    private val sakRepository: SakRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val behandlingRepository: BehandlingRepository,
) {
    fun hentDialogForSak(saksnummer: String, token: OidcToken): MeldingerResponse {
        val dialogmeldinger = hentDialogmeldingerFraDokumentinnhenting(saksnummer)
        val legeerklæringer = hentLegeerklæringerForSakFraDatabase(saksnummer)

        val journalpostIDerForDialogmeldinger = dialogmeldinger.mapNotNull { it.journalpostId }
        val journalpostIDerForHelsedokumenter =
            legeerklæringer.map { it.referanse.asJournalpostId.identifikator }

        val journalposter = hentBegrensetJournalposterFraDokumentinnhenting(
            journalpostIDerForDialogmeldinger + journalpostIDerForHelsedokumenter,
            token
        )

        val dialogmeldingerMedDokumentoversikt =
            lagMeldingMedDokumentoversiktForDialogmeldinger(dialogmeldinger, journalposter)
        val legeerklæringerMedDokumentoversikt =
            lagMeldingMedDokumentoversiktForLegeerklæringer(legeerklæringer, journalposter)

        val sorterteMeldinger = (dialogmeldingerMedDokumentoversikt + legeerklæringerMedDokumentoversikt)
            .sortedBy { it.melding.opprettetTidspunkt }

        return MeldingerResponse(
            meldinger = sorterteMeldinger,
            kommendeMeldinger = utledKommendeMeldingerForSak(
                dialogmeldinger = dialogmeldingerMedDokumentoversikt.map { it.melding },
                legeerklæringer = legeerklæringer
            )
        )
    }

    private fun utledKommendeMeldingerForSak(
        dialogmeldinger: List<MeldingDto>,
        legeerklæringer: Set<MottattDokument>
    ): List<KommendeMeldingDto> {
        val forespørslerSomIkkeErBesvart = utledUbesvarteForespørslerLegeerklæringInnenTidsfrist(dialogmeldinger, legeerklæringer)

        return forespørslerSomIkkeErBesvart.sortedBy { it.opprettetTidspunkt }.map { melding ->
            KommendeMeldingDto(
                bestillingId = requireNotNull(melding.dialogmeldingId) {
                    "Kan ikke sende påminnelse når bestillingId ikke finnes"
                },
                behandlerNavn = requireNotNull(melding.meldingFraNavn) {
                    "Navn på behandler må være satt for utgående dialogmelding"
                },
                påminnelseErAvbrutt = melding.påminnelseAvbrutt ?: false,
                påminnelseDato = melding.opprettetTidspunkt.toLocalDate().plusDays(DAGER_TIL_PÅMINNELSE)
            )
        }
    }

    private fun utledUbesvarteForespørslerLegeerklæringInnenTidsfrist(
        dialogmeldinger: List<MeldingDto>,
        legeerklæringer: Set<MottattDokument>
    ): List<MeldingDto> {
        val kandidaterForPåminnelse =
            dialogmeldinger.filter {
                it.dokumentasjonsType == no.nav.aap.behandlingsflyt.behandling.behandlerdialog.DokumentasjonType.L40
                        && it.innkommendeUtgående == InnkommendeUtgående.UTGÅENDE
                        && it.dialogmeldingId != null
                        && it.opprettetTidspunkt.toLocalDate().plusDays(DAGER_TIL_PÅMINNELSE) > java.time.LocalDate.now()
            }

        return kandidaterForPåminnelse.filter { melding ->
            val finnesLegeerklæringSomKomInnEtterBestilling = legeerklæringer.any { legeerklæring ->
                legeerklæring.mottattTidspunkt > melding.opprettetTidspunkt
            }
            !finnesLegeerklæringSomKomInnEtterBestilling
        }
    }

    private fun utledUbesvarteForespørslerLegeerklæringUavhengigAvTidsfrist(
        forespørsler: List<MeldingDto>,
        legeerklæringer: Set<MottattDokument>
    ): List<MeldingDto> {
        val filtreteForespørsler =
            forespørsler.filter {
                it.dokumentasjonsType == no.nav.aap.behandlingsflyt.behandling.behandlerdialog.DokumentasjonType.L40
                        && it.innkommendeUtgående == InnkommendeUtgående.UTGÅENDE
                        && it.dialogmeldingId != null
            }

        return filtreteForespørsler.filter {
            forespørsel -> ingenLegeerklæringMottattEtterBestilling(forespørsel, legeerklæringer)
        }
    }

    private fun ingenLegeerklæringMottattEtterBestilling(forespørsel: MeldingDto, legeerklæringer: Set<MottattDokument>): Boolean {
        val finnesLegeerklæringSomKomInnEtterBestilling = legeerklæringer.any { legeerklæring ->
            legeerklæring.mottattTidspunkt > forespørsel.opprettetTidspunkt
        }
        return !finnesLegeerklæringSomKomInnEtterBestilling
    }

    private fun hentDialogmeldingerFraDokumentinnhenting(saksnummer: String): List<no.nav.aap.dokumentinnhenting.kontrakt.FellesDialogmeldingDto> {
        return dokumentinnhentingGateway.hentDialogmeldingerForSak(
            HentDialogmeldingerForSakParams(saksnummer)
        )
    }

    private fun hentLegeerklæringerForSakFraDatabase(saksnummer: String): Set<MottattDokument> {
        val sak = sakRepository.hent(Saksnummer.fra(saksnummer))

        return mottattDokumentRepository.hentDokumenterAvType(
            sak.id,
            InnsendingType.LEGEERKLÆRING
        )
    }

    private fun hentBegrensetJournalposterFraDokumentinnhenting(
        journalpostIDer: List<String>,
        token: OidcToken,
    ): Map<String, BegrensetJournalpostDto> {
        val journalposter = dokumentinnhentingGateway.hentDokumentoversiktForJournalpostListe(
            HentDokumentoversiktJournalpostListeParams(journalpostIDer),
            token
        )

        val dokumentoversiktMap = HashMap<String, BegrensetJournalpostDto>()
        journalposter.journalposter
            .filter { it.journalpostId != null }
            .forEach { journalpost ->
                dokumentoversiktMap[journalpost.journalpostId!!] = journalpost
            }
        return dokumentoversiktMap
    }

    fun hentUbesvarteForespørslerOmLegeerklæringer(
        behandlingsReferanse: UUID,
        currentToken: OidcToken
    ): List<MeldingDto> {
        val forespørslerLegeerklæring = hentForespørslerOmLegeerklæringFraDokumentinnhenting(
            behandlingsReferanse = behandlingsReferanse,
            currentToken = currentToken
        )
        val forespørslerMeldingDto = forespørslerLegeerklæring.map { it.tilMeldingDto() }

        val saksnummer = hentSaksnummerFraBehandlingsReferanse(behandlingsReferanse)
        val legeerklæringer = hentLegeerklæringerForSakFraDatabase(saksnummer.toString())

        return utledUbesvarteForespørslerLegeerklæringUavhengigAvTidsfrist(forespørslerMeldingDto, legeerklæringer)
    }

    private fun hentForespørslerOmLegeerklæringFraDokumentinnhenting(
        behandlingsReferanse: UUID,
        currentToken: OidcToken
    ): List<FellesDialogmeldingDto> {
        return dokumentinnhentingGateway.hentLegeerklæringForespørslerForSak(
            HentLegeerklæringForespørslerForSakParams(behandlingsReferanse),
            currentToken
        )
    }

    private fun lagMeldingMedDokumentoversiktForDialogmeldinger(
        dialogmeldinger: List<FellesDialogmeldingDto>,
        journalposter: Map<String, BegrensetJournalpostDto>
    ): List<MeldingMedDokumenterDto> {
        return dialogmeldinger.map { dialogmelding ->
            val dokumentoversikt = journalposter[dialogmelding.journalpostId]

            MeldingMedDokumenterDto(
                melding = dialogmelding.tilMeldingDto(),
                dokumentIdListe = dokumentoversikt?.dokumenter?.map { it.tilResponseDto() }.orEmpty()
            )
        }
    }

    private fun lagMeldingMedDokumentoversiktForLegeerklæringer(
        legeerklæringer: Set<MottattDokument>,
        journalposter: Map<String, BegrensetJournalpostDto>
    ): List<MeldingMedDokumenterDto> {
        return legeerklæringer.map { helsedokument ->
            val journalpostId = helsedokument.referanse.asJournalpostId.identifikator
            val journalpost = journalposter[journalpostId]

            MeldingMedDokumenterDto(
                melding = MeldingDto(
                    innkommendeUtgående = InnkommendeUtgående.INNKOMMENDE,
                    meldingFraNavn = journalpost?.avsenderMottakerDto?.navn,
                    opprettetTidspunkt = helsedokument.mottattTidspunkt,
                    dokumentasjonsType = no.nav.aap.behandlingsflyt.behandling.behandlerdialog.DokumentasjonType.L40,
                    tekst = null,
                    meldingStatus = null,
                    journalpostId = journalpostId
                ),
                dokumentIdListe = journalpost?.dokumenter?.map { it.tilResponseDto() }.orEmpty()
            )
        }
    }

    private fun hentSaksnummerFraBehandlingsReferanse(behandlingsReferanse: UUID): Saksnummer {
            val behandling = behandlingRepository.hent(BehandlingReferanse(behandlingsReferanse))
            val sak = sakRepository.hent(behandling.sakId)
            return sak.saksnummer
    }

    private fun no.nav.aap.dokumentinnhenting.kontrakt.InnkommendeUtgående.tilResponseType(): InnkommendeUtgående {
        return when (this) {
            no.nav.aap.dokumentinnhenting.kontrakt.InnkommendeUtgående.INNKOMMENDE -> InnkommendeUtgående.INNKOMMENDE
            no.nav.aap.dokumentinnhenting.kontrakt.InnkommendeUtgående.UTGÅENDE -> InnkommendeUtgående.UTGÅENDE
        }
    }

    private fun MeldingStatusDto.tilResponseDto(): DialogmeldingLeveringStatus? {
        return when (this) {
            MeldingStatusDto.SENDT -> DialogmeldingLeveringStatus.SENDT
            MeldingStatusDto.LEVERT -> DialogmeldingLeveringStatus.LEVERT
            MeldingStatusDto.FEILET -> DialogmeldingLeveringStatus.FEILET
        }
    }

    private fun DokumentasjonType.tilResponseType(): no.nav.aap.behandlingsflyt.behandling.behandlerdialog.DokumentasjonType {
        return when (this) {
            DokumentasjonType.L40 -> no.nav.aap.behandlingsflyt.behandling.behandlerdialog.DokumentasjonType.L40
            DokumentasjonType.L8 -> no.nav.aap.behandlingsflyt.behandling.behandlerdialog.DokumentasjonType.L8
            DokumentasjonType.L120 -> no.nav.aap.behandlingsflyt.behandling.behandlerdialog.DokumentasjonType.L120
            DokumentasjonType.MELDING_FRA_NAV -> no.nav.aap.behandlingsflyt.behandling.behandlerdialog.DokumentasjonType.MELDING_FRA_NAV
            DokumentasjonType.RETUR_LEGEERKLÆRING -> no.nav.aap.behandlingsflyt.behandling.behandlerdialog.DokumentasjonType.RETUR_LEGEERKLÆRING
            DokumentasjonType.PURRING -> no.nav.aap.behandlingsflyt.behandling.behandlerdialog.DokumentasjonType.PURRING
        }
    }

    private fun no.nav.aap.dokumentinnhenting.kontrakt.BegrensetDokumentInfoDto.tilResponseDto(): DokumentInfoDto {
        return DokumentInfoDto(
            dokumentInfoId = dokumentInfoId,
            tittel = this.tittel,
        )
    }

    private fun FellesDialogmeldingDto.tilMeldingDto(): MeldingDto {
        return MeldingDto(
            dialogmeldingId = this.dialogmeldingReferanse,
            innkommendeUtgående = this.innkommendeUtgående.tilResponseType(),
            meldingFraNavn = this.meldingFraNavn,
            opprettetTidspunkt = this.opprettetTidspunkt,
            dokumentasjonsType = this.dokumentasjonsType?.tilResponseType(),
            tekst = this.tekst,
            meldingStatus = this.meldingStatus?.tilResponseDto(),
            journalpostId = this.journalpostId,
            påminnelseAvbrutt = this.automatiskPåminnelse?.let { !it }
        )
    }
}