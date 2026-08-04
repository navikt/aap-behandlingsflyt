package no.nav.aap.behandlingsflyt.behandling.meldekort

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class MeldekortServiceTest {

    private val fixedClock = Clock.fixed(Instant.parse("2025-04-01T00:00:00Z"), ZoneId.systemDefault())

    private val saksnummer = Saksnummer("1")
    private val behandlingId = BehandlingId(42L)
    private val meldeperiode = Periode(6 januar 2025, 19 januar 2025)

    private val meldekortRepository = mockk<MeldekortRepository>()
    private val underveisRepository = mockk<UnderveisRepository>()
    private val sakRepository = mockk<SakRepository>()
    private val mottattDokumentRepository = mockk<MottattDokumentRepository>(relaxed = true)
    private val flytJobbRepository = mockk<FlytJobbRepository>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>()
    private val journalføringService = mockk<JournalføringService>(relaxed = true)
    private val ansattInfoService = mockk<AnsattInfoService>(relaxed = true)
    private val mottattHendelseService = mockk<MottattHendelseService>(relaxed = true)

    private fun service() = MeldekortService(
        meldekortRepository = meldekortRepository,
        underveisRepository = underveisRepository,
        sakRepository = sakRepository,
        mottattDokumentRepository = mottattDokumentRepository,
        flytJobbRepository = flytJobbRepository,
        behandlingService = lazy { behandlingService },
        journalføringService = lazy { journalføringService },
        ansattInfoService = lazy { ansattInfoService },
        mottattHendelseService = lazy { mottattHendelseService },
        clock = fixedClock,
    )

    @Test
    fun `kaster feil når underveisgrunnlag mangler`() {
        stubSakOgBehandling()
        every { underveisRepository.hentHvisEksisterer(behandlingId) } returns null
        every { meldekortRepository.hentHvisEksisterer(behandlingId) } returns null

        val oppdaterMeldekort = oppdaterMeldekort(meldedato = 20 januar 2025)

        assertThatThrownBy { service().oppdaterMeldekort(oppdaterMeldekort) }
            .isInstanceOf(UgyldigForespørselException::class.java)
            .hasMessageContaining("Fant ikke underveisgrunnlag for behandlingen")
    }

    @Test
    fun `kaster feil når meldedato er før sluttdato for meldeperioden`() {
        stubSakOgBehandling()
        stubUnderveisgrunnlag(underveisperiode(Utfall.OPPFYLT, meldeperiode))
        every { meldekortRepository.hentHvisEksisterer(behandlingId) } returns null

        val oppdaterMeldekort = oppdaterMeldekort(meldedato = 18 januar 2025)

        assertThatThrownBy { service().oppdaterMeldekort(oppdaterMeldekort) }
            .isInstanceOf(UgyldigForespørselException::class.java)
            .hasMessageContaining("må være etter sluttdatoen for meldeperioden")
    }

    @Test
    fun `kaster feil når meldedato er lik sluttdato for meldeperioden`() {
        stubSakOgBehandling()
        stubUnderveisgrunnlag(underveisperiode(Utfall.OPPFYLT, meldeperiode))
        every { meldekortRepository.hentHvisEksisterer(behandlingId) } returns null

        val oppdaterMeldekort = oppdaterMeldekort(meldedato = meldeperiode.tom)

        assertThatThrownBy { service().oppdaterMeldekort(oppdaterMeldekort) }
            .isInstanceOf(UgyldigForespørselException::class.java)
            .hasMessageContaining("må være etter sluttdatoen for meldeperioden")
    }

    @Test
    fun `kaster feil når meldeperioden ikke er gyldig for vedtaket`() {
        stubSakOgBehandling()
        stubUnderveisgrunnlag(underveisperiode(Utfall.OPPFYLT, meldeperiode))
        every { meldekortRepository.hentHvisEksisterer(behandlingId) } returns null

        val ugyldigMeldeperiode = Periode(3 februar 2025, 16 februar 2025)
        val oppdaterMeldekort = oppdaterMeldekort(
            meldeperiode = ugyldigMeldeperiode,
            meldedato = 17 februar 2025,
        )

        assertThatThrownBy { service().oppdaterMeldekort(oppdaterMeldekort) }
            .isInstanceOf(UgyldigForespørselException::class.java)
            .hasMessageContaining("er ikke gyldig for vedtaket")
    }

    @Test
    fun `kaster feil når meldedato er før mottattdato for siste meldekort med timer`() {
        stubSakOgBehandling()
        stubUnderveisgrunnlag(underveisperiode(Utfall.OPPFYLT, meldeperiode))
        stubNyesteMeldekort(mottattTidspunkt = LocalDateTime.of(2025, 1, 25, 9, 0))

        val oppdaterMeldekort = oppdaterMeldekort(
            meldedato = 20 januar 2025,
            dager = setOf(DagDto(dato = 6 januar 2025, timerArbeidet = 5.0)),
        )

        assertThatThrownBy { service().oppdaterMeldekort(oppdaterMeldekort) }
            .isInstanceOf(UgyldigForespørselException::class.java)
            .hasMessageContaining("er før siste mottatte meldekort for perioden")
    }

    @Test
    fun `godtar meldedato før siste meldekort når det ikke registreres timer`() {
        stubSakOgBehandling()
        stubUnderveisgrunnlag(underveisperiode(Utfall.OPPFYLT, meldeperiode))
        stubNyesteMeldekort(mottattTidspunkt = LocalDateTime.of(2025, 1, 25, 9, 0))
        stubJournalføring(JournalpostId("journalpost-1"))

        val oppdaterMeldekort = oppdaterMeldekort(
            meldedato = 20 januar 2025,
            dager = emptySet(),
        )

        val resultat = service().oppdaterMeldekort(oppdaterMeldekort)

        assertThat(resultat.journalpostId).isEqualTo(JournalpostId("journalpost-1"))
    }

    @Test
    fun `godtar meldedato lik mottattdato for siste meldekort med timer`() {
        stubSakOgBehandling()
        stubUnderveisgrunnlag(underveisperiode(Utfall.OPPFYLT, meldeperiode))
        stubNyesteMeldekort(mottattTidspunkt = LocalDateTime.of(2025, 1, 20, 9, 0))
        stubJournalføring(JournalpostId("journalpost-2"))

        val oppdaterMeldekort = oppdaterMeldekort(
            meldedato = 20 januar 2025,
            dager = setOf(DagDto(dato = 6 januar 2025, timerArbeidet = 5.0)),
        )

        assertThatCode { service().oppdaterMeldekort(oppdaterMeldekort) }.doesNotThrowAnyException()
    }

    @Test
    fun `godtar gyldig meldekort og returnerer journalpostId`() {
        stubSakOgBehandling()
        stubUnderveisgrunnlag(underveisperiode(Utfall.OPPFYLT, meldeperiode))
        every { meldekortRepository.hentHvisEksisterer(behandlingId) } returns null
        stubJournalføring(JournalpostId("journalpost-3"))

        val oppdaterMeldekort = oppdaterMeldekort(
            meldedato = 20 januar 2025,
            dager = setOf(DagDto(dato = 6 januar 2025, timerArbeidet = 7.5)),
        )

        val resultat = service().oppdaterMeldekort(oppdaterMeldekort)

        assertThat(resultat.journalpostId).isEqualTo(JournalpostId("journalpost-3"))
    }

    private fun stubSakOgBehandling() {
        val sak = mockk<Sak>(relaxed = true)
        every { sakRepository.hent(saksnummer) } returns sak

        val behandling = mockk<BehandlingMedVedtak>(relaxed = true)
        every { behandling.id } returns behandlingId
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(any()) } returns behandling
    }

    private fun stubUnderveisgrunnlag(vararg perioder: Underveisperiode) {
        every { underveisRepository.hentHvisEksisterer(behandlingId) } returns
                UnderveisGrunnlag(id = 1L, perioder = perioder.toList())
    }

    private fun stubNyesteMeldekort(mottattTidspunkt: LocalDateTime) {
        val nyesteMeldekort = Meldekort(
            journalpostId = JournalpostId("eksisterende"),
            timerArbeidPerPeriode = emptySet(),
            mottattTidspunkt = mottattTidspunkt,
            opprettetTidspunkt = mottattTidspunkt,
        )
        val meldekortGrunnlag = mockk<MeldekortGrunnlag>()
        every { meldekortGrunnlag.nyesteForMeldeperiode(any()) } returns nyesteMeldekort
        every { meldekortGrunnlag.nyesteForMeldeperiodePåDato(any(), any()) } returns null
        every { meldekortRepository.hentHvisEksisterer(behandlingId) } returns meldekortGrunnlag
    }

    private fun stubJournalføring(journalpostId: JournalpostId) {
        every {
            journalføringService.journalfør(any(), any(), any(), any(), any(), any(), any(), any())
        } returns journalpostId
    }

    private fun oppdaterMeldekort(
        meldedato: LocalDate,
        meldeperiode: Periode = this.meldeperiode,
        dager: Set<DagDto> = emptySet(),
    ) = OppdaterMeldekort(
        saksnummer = saksnummer,
        meldeperiode = meldeperiode,
        meldedato = meldedato,
        begrunnelse = "Registrering av timer",
        dager = dager,
        bruker = Bruker("saksbehandler"),
    )

    private fun underveisperiode(
        utfall: Utfall,
        periode: Periode,
        meldePeriode: Periode = periode,
    ) = Underveisperiode(
        periode = periode,
        meldePeriode = meldePeriode,
        utfall = utfall,
        rettighetsType = RettighetsType.BISTANDSBEHOV,
        avslagsårsak = if (utfall == Utfall.IKKE_OPPFYLT) UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT_11_7_STANS else null,
        grenseverdi = Prosent.`100_PROSENT`,
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
            andelArbeid = `0_PROSENT`,
            fastsattArbeidsevne = Prosent.`100_PROSENT`,
            gradering = Prosent.`100_PROSENT`,
            opplysningerMottatt = null,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = emptySet(),
        institusjonsoppholdReduksjon = `0_PROSENT`,
        meldepliktStatus = MeldepliktStatus.MELDT_SEG,
        meldepliktGradering = `0_PROSENT`,
    )
}
