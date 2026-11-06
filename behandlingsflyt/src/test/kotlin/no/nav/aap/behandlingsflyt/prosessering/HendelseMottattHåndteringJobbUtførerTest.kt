package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.hendelse.mottak.HåndterMottattDokumentService
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.SakSkrivelås
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime

class HendelseMottattHåndteringJobbUtførerTest {
    private val sakId = SakId(1)
    private val saksnummer = Saksnummer("123")
    private val referanse = InnsendingReferanse(InnsendingReferanse.Type.REVURDERING_ID, "revurdering")
    private val mottattTidspunkt = LocalDateTime.of(2026, 1, 2, 12, 30)
    private val brevkategori = InnsendingType.MANUELL_REVURDERING
    private val kanal = Kanal.DIGITAL
    private val mottattDokumentRepository = mockk<MottattDokumentRepository>(relaxUnitFun = true)
    private val håndterMottattDokumentService = mockk<HåndterMottattDokumentService>(relaxUnitFun = true)
    private val låsRepository = mockk<TaSkriveLåsRepository>(relaxUnitFun = true)
    private val skrivelås = SakSkrivelås(sakId, 1)
    private val utfører = HendelseMottattHåndteringJobbUtfører(
        låsRepository = låsRepository,
        håndterMottattDokumentService = håndterMottattDokumentService,
        mottaDokumentService = MottaDokumentService(mottattDokumentRepository),
        mottattDokumentRepository = mottattDokumentRepository,
        håndterKlageService = mockk(),
        håndterTilbakekrevingHendelse = mockk(),
        håndterDialogMeldingService = mockk(),
        håndterVedtakHendelseService = mockk(),
        håndterUførevedtakService = mockk(),
        håndterFolkeregisterIdentHendelseService = mockk(),
        behandlingService = mockk(),
        trukketSøknadService = mockk(),
    )

    @BeforeEach
    fun setUp() {
        every { mottattDokumentRepository.hentDokumenterAvType(sakId, brevkategori) } returns emptySet()
        every { låsRepository.låsSak(sakId) } returns skrivelås
    }

    @ParameterizedTest
    @CsvSource(
        "Z123456,null",
        "null,null",
        "Z123456,Z654321",
        "null,Z654321",
        nullValues = ["null"]
    )
    fun `innsending bevarer nytt felt opprettetAv`(
        opprettetAv: String?,
        legagyOpprettetAv: String?
    ) {
        val melding = melding(legagyOpprettetAv)
        val sakRepository = mockk<SakRepository>()
        val sak = mockk<Sak> { every { id } returns sakId }
        val flytJobbRepository = mockk<FlytJobbRepository>(relaxUnitFun = true)
        every { sakRepository.hent(saksnummer) } returns sak
        val mottaksservice = MottattHendelseService(
            sakRepository = sakRepository,
            mottattDokumentRepository = mottattDokumentRepository,
            flytJobbRepository = flytJobbRepository,
            behandlingRepository = mockk(),
        )

        mottaksservice.registrerMottattHendelse(
            Innsending(
                saksnummer = saksnummer,
                referanse = referanse,
                type = brevkategori,
                kanal = kanal,
                mottattTidspunkt = mottattTidspunkt,
                melding = melding,
                digitalisertAvPostmottak = true,
                opprettetAv = opprettetAv,
            )
        )

        val jobb = slot<JobbInput>()
        verify(exactly = 1) { flytJobbRepository.leggTil(capture(jobb)) }
        assertThat(jobb.captured.optionalParameter("opprettetAv")).isEqualTo(opprettetAv)

        utfører.utfør(jobb.captured)

        verifiserLagretOgVideresendt(melding, opprettetAv, digitalisertAvPostmottak = true)
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["Z123456"])
    fun `nyJobb lagrer NAV-ident som parameter og utelater parameter ved null`(opprettetAv: String?) {
        val jobb = HendelseMottattHåndteringJobbUtfører.nyJobb(
            sakId = sakId,
            dokumentReferanse = referanse,
            brevkategori = brevkategori,
            kanal = kanal,
            melding = melding(null),
            mottattTidspunkt = mottattTidspunkt,
            opprettetAv = opprettetAv,
        )

        assertThat(jobb.optionalParameter("opprettetAv")).isEqualTo(opprettetAv)

        utfører.utfør(jobb)

        verifiserLagretOgVideresendt(melding(null), opprettetAv, digitalisertAvPostmottak = false)
    }

    @Test
    fun `nyJobb uten opprettetAv bruker bakoverkompatibel standardverdi`() {
        val jobb = HendelseMottattHåndteringJobbUtfører.nyJobb(
            sakId = sakId,
            dokumentReferanse = referanse,
            brevkategori = brevkategori,
            kanal = kanal,
            melding = melding("Z654321"),
            mottattTidspunkt = mottattTidspunkt,
        )

        assertThat(jobb.optionalParameter("opprettetAv")).isNull()

        utfører.utfør(jobb)

        verifiserLagretOgVideresendt(melding("Z654321"), null, digitalisertAvPostmottak = false)
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["Z654321"])
    fun `gammel jobb uten opprettetAv-parameter fungerer`(
        legagyOpprettetAv: String?
    ) {
        val melding = melding(legagyOpprettetAv)
        // Bygg gammelt jobbformat uten nyJobb, slik at testen ikke avhenger av dagens standardverdier.
        val gammelJobb = JobbInput(HendelseMottattHåndteringJobbUtfører)
            .forSak(sakId.toLong())
            .medParameter("referanse", DefaultJsonMapper.toJson(referanse))
            .medParameter("brevkode", brevkategori.name)
            .medParameter("kanal", kanal.name)
            .medParameter("mottattTidspunkt", DefaultJsonMapper.toJson(mottattTidspunkt))
            .medPayload(melding)

        assertThat(gammelJobb.optionalParameter("opprettetAv")).isNull()

        utfører.utfør(gammelJobb)

        verifiserLagretOgVideresendt(melding, null, digitalisertAvPostmottak = null)
    }

    private fun verifiserLagretOgVideresendt(
        melding: ManuellRevurderingV0,
        opprettetAv: String?,
        digitalisertAvPostmottak: Boolean?
    ) {
        val dokument = slot<MottattDokument>()
        verify(exactly = 1) { mottattDokumentRepository.lagre(capture(dokument)) }
        assertThat(dokument.captured.opprettetAv).isEqualTo(opprettetAv)
        assertThat(dokument.captured.sakId).isEqualTo(sakId)
        assertThat(dokument.captured.referanse).isEqualTo(referanse)
        assertThat(dokument.captured.mottattTidspunkt).isEqualTo(mottattTidspunkt)
        assertThat(dokument.captured.type).isEqualTo(brevkategori)
        assertThat(dokument.captured.kanal).isEqualTo(kanal)
        assertThat(dokument.captured.status).isEqualTo(Status.MOTTATT)
        assertThat(dokument.captured.behandlingId).isNull()
        assertThat(dokument.captured.digitalisertAvPostmottak).isEqualTo(digitalisertAvPostmottak)
        assertThat(dokument.captured.ustrukturerteData()).isEqualTo(DefaultJsonMapper.toJson(melding))
        verify(exactly = 1) {
            håndterMottattDokumentService.håndterMottatteDokumenter(
                sakId, referanse, mottattTidspunkt, brevkategori, melding, opprettetAv
            )
        }
        verifyOrder {
            mottattDokumentRepository.lagre(any())
            håndterMottattDokumentService.håndterMottatteDokumenter(
                sakId, referanse, mottattTidspunkt, brevkategori, melding, opprettetAv
            )
            låsRepository.verifiserSkrivelås(skrivelås)
        }
    }

    private fun melding(opprettetAv: String?) = ManuellRevurderingV0(
        årsakerTilBehandling = listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
        beskrivelse = "Revurdering",
        opprettetAv = opprettetAv,
    )
}
