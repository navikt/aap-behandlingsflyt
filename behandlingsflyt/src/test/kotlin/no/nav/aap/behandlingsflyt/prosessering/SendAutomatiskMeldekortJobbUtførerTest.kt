package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.TestAutomatiskMeldekortSakRepository
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SendAutomatiskMeldekortJobbUtførerTest {

    private val sakId = SakId(12345L)
    private val jobbInput = JobbInput(SendAutomatiskMeldekortJobbUtfører).forSak(sakId.toLong())

    private val sakRepository = FakeTestAutomatiskMeldekortSakRepository()
    private val underveisRepository = FakeUnderveisRepository()
    private val flytJobbRepository = FlytJobbFake()

    @BeforeEach
    fun setup() {
        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
    }

    @Test
    fun `sender meldekort for ubesvart meldeperiode`() {
        val idag = LocalDate.of(2026, 6, 9)
        val periode = Periode(idag.minusDays(14), idag.minusDays(1))
        sakRepository.leggTil(sakId)
        underveisRepository.settUbesvarte(sakId, listOf(periode))

        lagUtfører(idag).utfør(jobbInput)

        assertThat(flytJobbRepository.hentJobberForSak(sakId.toLong())).hasSize(1)
    }

    @Test
    fun `sender ikke meldekort når det ikke finnes ubesvarte meldeperioder`() {
        val idag = LocalDate.of(2026, 6, 9)
        sakRepository.leggTil(sakId)

        lagUtfører(idag).utfør(jobbInput)

        assertThat(flytJobbRepository.hentJobberForSak(sakId.toLong())).isEmpty()
    }

    @Test
    fun `meldekort-jobb opprettes med referanse av type JOURNALPOST`() {
        val idag = LocalDate.of(2026, 6, 9)
        val periode = Periode(idag.minusDays(14), idag.minusDays(1))
        sakRepository.leggTil(sakId)
        underveisRepository.settUbesvarte(sakId, listOf(periode))

        lagUtfører(idag).utfør(jobbInput)

        val jobb = flytJobbRepository.hentJobberForSak(sakId.toLong()).single()
        val referanse = DefaultJsonMapper.fromJson<InnsendingReferanse>(jobb.parameter("referanse"))
        assertThat(referanse.type).isEqualTo(InnsendingReferanse.Type.JOURNALPOST)
    }

    @Test
    fun `sender ett meldekort per ubesvart meldeperiode`() {
        val idag = LocalDate.of(2026, 6, 9)
        val periode1 = Periode(idag.minusDays(42), idag.minusDays(29))
        val periode2 = Periode(idag.minusDays(28), idag.minusDays(15))
        val periode3 = Periode(idag.minusDays(14), idag.minusDays(1))
        sakRepository.leggTil(sakId)
        underveisRepository.settUbesvarte(sakId, listOf(periode1, periode2, periode3))

        lagUtfører(idag).utfør(jobbInput)

        assertThat(flytJobbRepository.hentJobberForSak(sakId.toLong())).hasSize(3)
    }

    @Test
    fun `sender meldekort for flere saker`() {
        val idag = LocalDate.of(2026, 6, 9)
        val annenSakId = SakId(99999L)
        val periode = Periode(idag.minusDays(14), idag.minusDays(1))
        sakRepository.leggTil(sakId)
        sakRepository.leggTil(annenSakId)
        underveisRepository.settUbesvarte(sakId, listOf(periode))
        underveisRepository.settUbesvarte(annenSakId, listOf(periode))

        lagUtfører(idag).utfør(jobbInput)

        assertThat(flytJobbRepository.hentJobberForSak(sakId.toLong())).hasSize(1)
        assertThat(flytJobbRepository.hentJobberForSak(annenSakId.toLong())).hasSize(1)
    }

    private fun lagUtfører(idag: LocalDate) = SendAutomatiskMeldekortJobbUtfører(
        automatiskMeldekortSakRepository = sakRepository,
        underveisRepository = underveisRepository,
        flytJobbRepository = flytJobbRepository,
        clock = fixedClock(idag),
    )

    private class FakeTestAutomatiskMeldekortSakRepository : TestAutomatiskMeldekortSakRepository {
        private val saker = mutableListOf<SakId>()

        override fun leggTil(sakId: SakId) { saker.add(sakId) }
        override fun eksisterer(sakId: SakId) = sakId in saker
        override fun hentAlle() = saker.toList()
        override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) = Unit
        override fun slett(behandlingId: BehandlingId) = Unit
    }

    private class FakeUnderveisRepository : UnderveisRepository {
        private val ubesvarte = mutableMapOf<SakId, List<Periode>>()

        fun settUbesvarte(sakId: SakId, perioder: List<Periode>) { ubesvarte[sakId] = perioder }

        override fun hentUbesvarteMeldeperioderForDollyJobb(sakIds: List<SakId>, idag: LocalDate) =
            ubesvarte.filterKeys { it in sakIds }

        override fun hent(behandlingId: BehandlingId): UnderveisGrunnlag = TODO()
        override fun hentHvisEksisterer(behandlingId: BehandlingId): UnderveisGrunnlag? = null
        override fun lagre(behandlingId: BehandlingId, underveisperioder: List<Underveisperiode>, input: Faktagrunnlag) = Unit
        override fun hentSakerMedSisteUnderveisperiodeFørDato(sisteUnderveisDato: LocalDate): Set<SakId> = emptySet()
        override fun hentSakerForGRegulering(datoForGJustering: LocalDate, nyttGrunnbeløp: Beløp): Set<SakId> = emptySet()
        override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) = Unit
        override fun slett(behandlingId: BehandlingId) = Unit
    }

    private class FlytJobbFake : FlytJobbRepository {
        private val jobber = mutableListOf<JobbInput>()

        override fun leggTil(jobbInput: JobbInput) { jobber.add(jobbInput) }
        override fun hentJobberForBehandling(id: Long) = jobber.filter { it.behandlingIdOrNull() == id }
        override fun hentJobberForSak(id: Long) = jobber.filter { it.sakId() == id }
        override fun hentFeilmeldingForOppgave(id: Long) = ""
    }
}


