package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumentinnhentingGateway
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository.opprettBehandling
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.dokumentinnhenting.kontrakt.AvsenderMottakerDto
import no.nav.aap.dokumentinnhenting.kontrakt.BegrensetDokumentInfoDto
import no.nav.aap.dokumentinnhenting.kontrakt.BegrensetJournalpostDto
import no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType
import no.nav.aap.dokumentinnhenting.kontrakt.FellesDialogmeldingDto
import no.nav.aap.dokumentinnhenting.kontrakt.HentDialogmeldingerForSakParams
import no.nav.aap.dokumentinnhenting.kontrakt.HentDokumentoversiktJournalpostListeParams
import no.nav.aap.dokumentinnhenting.kontrakt.HentDokumentoversiktJournalpostListeResponse
import no.nav.aap.dokumentinnhenting.kontrakt.InnkommendeUtgående
import no.nav.aap.dokumentinnhenting.kontrakt.MeldingStatusDto
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class HentBehandlerDialogServiceTest {

    private val dokumentinnhentingGateway = mockk<DokumentinnhentingGateway>()
    private val dataSource = MockDataSource()
    private val service = HentBehandlerDialogService(dataSource, dokumentinnhentingGateway, inMemoryRepositoryRegistry)

    @Test
    fun `hente ut dialogmeldinger og legeerklæring`() {
        // arrange
        val sak = opprettInMemorySak()
        val behandling = opprettBehandling(
            sak.id,
            TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(listOf(), ÅrsakTilOpprettelse.SØKNAD)
        )

        val saksnummer = sak.saksnummer.toString()
        val journalpostIdSendtMelding1 = "123"
        val journalpostIdSendtMelding2 = "456"
        val journalpostIdMottattMelding = "789"
        val navnSaksbehandler = "Saksbehandler hos NAV"
        val tekstFørsteMelding = "Hei, kan dere sende over legeerklæring for pasienten?"

        InMemoryMottattDokumentRepository.lagre(
            lagLegeerklæring(journalpostIdMottattMelding, sak, behandling)
        )

        val dialogmeldingerForSakResponse = listOf(
            FellesDialogmeldingDto(
                innkommendeUtgående = InnkommendeUtgående.UTGÅENDE,
                meldingFraNavn = navnSaksbehandler,
                opprettetTidspunkt = LocalDateTime.now().minusDays(31),
                dokumentasjonsType = DokumentasjonType.MELDING_FRA_NAV,
                tekst = tekstFørsteMelding,
                meldingStatus = MeldingStatusDto.LEVERT,
                journalpostId = journalpostIdSendtMelding1
            ),
            FellesDialogmeldingDto(
                innkommendeUtgående = InnkommendeUtgående.UTGÅENDE,
                meldingFraNavn = navnSaksbehandler,
                opprettetTidspunkt = LocalDateTime.now().minusDays(10),
                dokumentasjonsType = DokumentasjonType.PURRING,
                tekst = null,
                meldingStatus = MeldingStatusDto.LEVERT,
                journalpostId = journalpostIdSendtMelding2
            )
        )
        val hentDokumentlisteResponse = HentDokumentoversiktJournalpostListeResponse(
            journalposter = listOf(
                lagBegrensetJournalpostDto(journalpostIdSendtMelding1),
                lagBegrensetJournalpostDto(journalpostIdSendtMelding2),
                lagBegrensetJournalpostDto(journalpostIdMottattMelding, "Fastlege", "Legeerklæring")
            )
        )

        every {
            dokumentinnhentingGateway.hentDialogmeldingerForSak(HentDialogmeldingerForSakParams(saksnummer))
        } returns dialogmeldingerForSakResponse
        every {
            dokumentinnhentingGateway.hentDokumentoversiktForJournalpostListe(
                request = HentDokumentoversiktJournalpostListeParams(listOf(
                    journalpostIdSendtMelding1, journalpostIdSendtMelding2, journalpostIdMottattMelding)
                )
            )
        } returns hentDokumentlisteResponse

        // act
        val result = service.hentDialogForSak(saksnummer)

        // assert
        assertThat(result.size).isEqualTo(3)
        assertThat(result[0].melding.journalpostId).isEqualTo(journalpostIdSendtMelding1)
        assertThat(result[1].melding.journalpostId).isEqualTo(journalpostIdSendtMelding2)
        assertThat(result[2].melding.journalpostId).isEqualTo(journalpostIdMottattMelding)
        assertThat(result[0].melding.tekst).isEqualTo(tekstFørsteMelding)
        assertThat(result[2].dokumentIdListe.size).isEqualTo(1)
        assertThat(result[2].dokumentIdListe[0].tittel).isEqualTo("Legeerklæring")
    }

    private fun lagLegeerklæring(journalpostId: String, sak: Sak, behandling: Behandling): MottattDokument {
        return MottattDokument(
            InnsendingReferanse(
                type = InnsendingReferanse.Type.JOURNALPOST,
                verdi = journalpostId
            ),
            sak.id,
            behandling.id,
            mottattTidspunkt = LocalDateTime.now().minusDays(3),
            opprettetTid = LocalDateTime.now().minusDays(4),
            type = InnsendingType.LEGEERKLÆRING,
            kanal = Kanal.DIGITAL,
            status = Status.MOTTATT,
            strukturertDokument = null,
            digitalisertAvPostmottak = false
        )
    }

    private fun lagBegrensetJournalpostDto(
        journalpostId: String,
        navn: String = "Saksbehandler hos NAV",
        tittel: String = "Melding"
    ): BegrensetJournalpostDto {
        return BegrensetJournalpostDto(
            journalpostId = journalpostId,
            dokumenter = listOf(BegrensetDokumentInfoDto(
                dokumentInfoId = "99999",
                tittel = tittel,
            )),
            avsenderMottakerDto = AvsenderMottakerDto(
                id = "123456",
                type = null,
                navn = navn,
            )
        )
    }
}