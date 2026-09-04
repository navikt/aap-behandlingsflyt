package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

import no.nav.aap.behandlingsflyt.behandling.krav.tilSøknadUtenKravDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumentinnhentingGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.dokumentinnhenting.kontrakt.BegrensetJournalpostDto
import no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType
import no.nav.aap.dokumentinnhenting.kontrakt.HentDialogmeldingerForSakParams
import no.nav.aap.dokumentinnhenting.kontrakt.HentDokumentoversiktJournalpostListeParams
import no.nav.aap.dokumentinnhenting.kontrakt.MeldingStatusDto
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import javax.sql.DataSource

class HentBehandlerDialogService(
    private val dataSource: DataSource,
    private val dokumentinnhentingGateway: DokumentinnhentingGateway,
    private val repositoryRegistry: RepositoryRegistry,
) {
    fun hentDialogForSak(saksnummer: String): List<MeldingMedDokumenterDto> {
        val dialogmeldinger = hentDialogmeldingerFraDokumentinnhenting(saksnummer)
        val legeerklæringer = `hentLegeerklæringerForSakFraDatabase`(saksnummer)

        val journalpostIDerForDialogmeldinger = dialogmeldinger.mapNotNull { it.journalpostId }
        val journalpostIDerForHelsedokumenter = legeerklæringer.map { it.tilSøknadUtenKravDto().journalpostId.toString() }

        val journalposter = hentBegrensetJournalposterFraDokumentinnhenting(
            journalpostIDerForDialogmeldinger + journalpostIDerForHelsedokumenter,
        )

        val dialogmeldingerMedDokumentoversikt = lagMeldingMedDokumentoversiktForDialogmeldinger(dialogmeldinger, journalposter)
        val legeerklæringerMedDokumentoversikt = lagMeldingMedDokumentoversiktForLegeerklæringer(legeerklæringer, journalposter)

        return dialogmeldingerMedDokumentoversikt + legeerklæringerMedDokumentoversikt
    }

    private fun hentDialogmeldingerFraDokumentinnhenting(saksnummer: String): List<no.nav.aap.dokumentinnhenting.kontrakt.FellesDialogmeldingDto> {
        return dokumentinnhentingGateway.hentDialogmeldingerForSak(
            HentDialogmeldingerForSakParams(saksnummer)
        )
    }

    private fun `hentLegeerklæringerForSakFraDatabase`(saksnummer: String): Set<MottattDokument> {
        return dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val sak = repositoryProvider.provide<SakRepository>().hent(Saksnummer.fra(saksnummer))

            val mottattDokumentRepository = repositoryProvider.provide<MottattDokumentRepository>()

            return@transaction mottattDokumentRepository.hentDokumenterAvType(
                sak.id,
                InnsendingType.LEGEERKLÆRING
            )
        }
    }

    private fun hentBegrensetJournalposterFraDokumentinnhenting(
        journalpostIDer: List<String>
    ) : Map<String, BegrensetJournalpostDto> {
        val journalposter = dokumentinnhentingGateway.hentDokumentoversiktForJournalpostListe(
            // TODO: Endre til Kontrakt-versjon!
            HentDokumentoversiktJournalpostListeParams(
                journalpostIDer
            )
        )

        val dokumentoversiktMap = HashMap<String, BegrensetJournalpostDto>()
        journalposter.journalposter
            .filter { it.journalpostId != null }
            .forEach { journalpost ->
                dokumentoversiktMap[journalpost.journalpostId!!] = journalpost
            }
        return dokumentoversiktMap
    }

    private fun lagMeldingMedDokumentoversiktForDialogmeldinger(
        dialogmeldinger: List<no.nav.aap.dokumentinnhenting.kontrakt.FellesDialogmeldingDto>,
        journalposter: Map<String, BegrensetJournalpostDto>
    ): List<MeldingMedDokumenterDto> {
        return dialogmeldinger.map { dialogmelding ->
            val dokumentoversikt = journalposter[dialogmelding.journalpostId]

            MeldingMedDokumenterDto(
                melding = MeldingDto(
                    `innkommendeUtgående` = dialogmelding.innkommendeUtgående.tilResponseType(),
                    meldingFraNavn = dialogmelding.meldingFraNavn,
                    opprettetTidspunkt = dialogmelding.opprettetTidspunkt,
                    dokumentasjonsType = dialogmelding.dokumentasjonsType,
                    tekst = dialogmelding.tekst,
                    meldingStatus = dialogmelding.meldingStatus?.tilResponseDto(),
                    journalpostId = dialogmelding.journalpostId,
                ),
                dokumentIdListe = dokumentoversikt?.dokumenter?.map { it.tilResponseDto() }.orEmpty()
            )
        }
    }

    private fun lagMeldingMedDokumentoversiktForLegeerklæringer(
        `legeerklæringer`: Set<MottattDokument>,
        journalposter: Map<String, BegrensetJournalpostDto>
    ): List<MeldingMedDokumenterDto> {
        return `legeerklæringer`.map { helsedokument ->
            val journalpostId = helsedokument.tilSøknadUtenKravDto().journalpostId.toString()
            val journalpost = journalposter[journalpostId]

            MeldingMedDokumenterDto(
                melding = MeldingDto(
                    `innkommendeUtgående` = `InnkommendeUtgående`.INNKOMMENDE,
                    meldingFraNavn = journalpost?.avsenderMottakerDto?.navn ?: "",
                    opprettetTidspunkt = helsedokument.mottattTidspunkt,
                    dokumentasjonsType = DokumentasjonType.L40,
                    tekst = "",
                    meldingStatus = null,
                    journalpostId = journalpostId
                ),
                dokumentIdListe = journalpost?.dokumenter?.map { it.tilResponseDto() }.orEmpty()
            )
        }
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

    private fun no.nav.aap.dokumentinnhenting.kontrakt.BegrensetDokumentInfoDto.tilResponseDto(): DokumentInfoDto {
        return DokumentInfoDto(
            dokumentInfoId = dokumentInfoId,
            tittel = this.tittel,
        )
    }
}