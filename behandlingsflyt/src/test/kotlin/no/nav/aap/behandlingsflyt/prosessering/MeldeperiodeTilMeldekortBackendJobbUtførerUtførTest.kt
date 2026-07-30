package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMeldeperiodeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMeldepliktRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVedtakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryservice.InMemoryBehandlingService
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class MeldeperiodeTilMeldekortBackendJobbUtførerUtførTest {

    private val sakService = SakService(InMemorySakRepository, InMemoryBehandlingRepository)
    private val trukketSøknadService = TrukketSøknadService(InMemoryTrukketSøknadRepository)
    private val vedtakService =
        VedtakService(InMemoryVedtakRepository, InMemoryBehandlingRepository, InMemoryUnderveisRepository)

    private val unleashGateway = mockk<UnleashGateway>()
    private val meldekortGateway = mockk<MeldekortGateway>()

    private val utfører = MeldeperiodeTilMeldekortBackendJobbUtfører(
        sakService, InMemoryBehandlingService, meldekortGateway, InMemoryBehandlingRepository,
        InMemoryMeldeperiodeRepository, InMemoryUnderveisRepository, InMemoryMeldepliktRepository,
        trukketSøknadService, vedtakService, unleashGateway
    )

    init {
        every { meldekortGateway.oppdaterMeldeperioder(any()) } returns Unit
    }

    @Test
    fun `feature toggle av - sender data basert på triggerende behandling`() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.MeldeperiodeTilMeldekortBackendBasertPaaGjeldendeYtelsesbehandling) } returns false
        val førstegangsbehandling = opprettFørstegangsbehandling()
        val revurdering = opprettRevurdering(førstegangsbehandling)

        lagreUnderveisperiodeMedRett(
            førstegangsbehandling.id,
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2026, 1, 1)
        )
        lagreUnderveisperiodeMedRett(revurdering.id, fom = LocalDate.of(2025, 2, 1), tom = LocalDate.of(2026, 1, 1))

        iverksettBehandling(førstegangsbehandling)
        iverksettBehandling(revurdering)

        val sendt = utfør(førstegangsbehandling)

        assertThat(sendt?.opplysningsbehov).singleElement()
            .matches { it.fom == LocalDate.of(2025, 1, 1) && it.tom == LocalDate.of(2026, 1, 1) }
    }

    @Test
    fun `feature toggle på - sender data basert på gjeldende ytelsesbehandling når den er nyere enn behandlingen som trigget jobben`() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.MeldeperiodeTilMeldekortBackendBasertPaaGjeldendeYtelsesbehandling) } returns true
        val førstegangsbehandling = opprettFørstegangsbehandling()
        val revurdering = opprettRevurdering(førstegangsbehandling)

        lagreUnderveisperiodeMedRett(
            førstegangsbehandling.id,
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2026, 1, 1)
        )
        lagreUnderveisperiodeMedRett(revurdering.id, fom = LocalDate.of(2025, 2, 1), tom = LocalDate.of(2026, 1, 1))

        iverksettBehandling(førstegangsbehandling)
        iverksettBehandling(revurdering)

        val sendt = utfør(førstegangsbehandling)

        assertThat(sendt?.opplysningsbehov).singleElement()
            .matches { it.fom == LocalDate.of(2025, 2, 1) && it.tom == LocalDate.of(2026, 1, 1) }
    }

    @Test
    fun `feature toggle på - ingen gjeldende ytelsesbehandling, faller tilbake til behandlingen som trigget jobben`() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.MeldeperiodeTilMeldekortBackendBasertPaaGjeldendeYtelsesbehandling) } returns true
        val førstegangsbehandling = opprettFørstegangsbehandling()
        lagreUnderveisperiodeMedRett(
            førstegangsbehandling.id,
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2026, 1, 1)
        )

        val sendt = utfør(førstegangsbehandling)

        // Før vedtak
        assertThat(sendt?.opplysningsbehov).singleElement()
            .matches { it.fom == LocalDate.now() && it.tom == Tid.MAKS }
    }

    @Test
    fun `feature toggle på - gjeldende ytelsesbehandling er samme som behandlingen som trigget jobben`() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.MeldeperiodeTilMeldekortBackendBasertPaaGjeldendeYtelsesbehandling) } returns true
        val førstegangsbehandling = opprettFørstegangsbehandling()

        lagreUnderveisperiodeMedRett(
            førstegangsbehandling.id,
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2026, 1, 1)
        )

        iverksettBehandling(førstegangsbehandling)

        val sendt = utfør(førstegangsbehandling)

        assertThat(sendt?.opplysningsbehov).singleElement()
            .matches { it.fom == LocalDate.of(2025, 1, 1) && it.tom == LocalDate.of(2026, 1, 1) }
    }

    @Test
    fun `feature toggle på - sender ikke data når triggende behandling er en revurdering som utredes`() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.MeldeperiodeTilMeldekortBackendBasertPaaGjeldendeYtelsesbehandling) } returns true
        val førstegangsbehandling = opprettFørstegangsbehandling()
        val revurdering = opprettRevurdering(førstegangsbehandling)

        lagreUnderveisperiodeMedRett(
            førstegangsbehandling.id,
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2026, 1, 1)
        )
        lagreUnderveisperiodeMedRett(revurdering.id, fom = LocalDate.of(2025, 2, 1), tom = LocalDate.of(2026, 1, 1))

        iverksettBehandling(førstegangsbehandling)

        val sendt = utfør(revurdering)

        assertThat(sendt).isNull()
    }


    private fun opprettFørstegangsbehandling(): Behandling {
        val sakOgBehandling = opprettInMemorySakOgBehandling()
        return sakOgBehandling.second
    }

    private fun iverksettBehandling(behandling: Behandling) {
        InMemoryBehandlingRepository.oppdaterBehandlingStatus(
            behandling.id,
            Status.IVERKSETTES
        )
    }

    private fun opprettRevurdering(behandling: Behandling): Behandling {
        return InMemoryBehandlingRepository.opprettBehandling(
            behandling.sakId,
            TypeBehandling.Revurdering,
            behandling.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.BARNETILLEGG)),
                årsak = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE,
            )
        )
    }

    private fun utfør(behandling: Behandling): MeldeperioderV0? {
        val sendt = slot<MeldeperioderV0>()
        every { meldekortGateway.oppdaterMeldeperioder(capture(sendt)) } returns Unit
        utfører.utfør(MeldeperiodeTilMeldekortBackendJobbUtfører.nyJobb(behandling.sakId, behandling.id))
        return if (sendt.isCaptured) sendt.captured else null
    }

    private fun lagreUnderveisperiodeMedRett(behandlingId: BehandlingId, fom: LocalDate, tom: LocalDate) {
        InMemoryUnderveisRepository.lagre(
            behandlingId,
            listOf(underveisperiodeMedRett(fom, tom)),
            object : Faktagrunnlag {})
    }

    private fun underveisperiodeMedRett(fom: LocalDate, tom: LocalDate) = Underveisperiode(
        periode = Periode(fom, tom),
        meldePeriode = Periode(fom, tom),
        utfall = Utfall.OPPFYLT,
        rettighetsType = RettighetsType.BISTANDSBEHOV,
        avslagsårsak = null,
        grenseverdi = Prosent(60),
        institusjonsoppholdReduksjon = Prosent(0),
        arbeidsgradering = nullArbeidsgradering(),
        trekk = Dagsatser(0),
        brukerAvKvoter = emptySet(),
        meldepliktStatus = MeldepliktStatus.MELDT_SEG,
        meldepliktGradering = Prosent.`100_PROSENT`,
    )

    private fun nullArbeidsgradering() = ArbeidsGradering(
        totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
        andelArbeid = Prosent.`0_PROSENT`,
        fastsattArbeidsevne = Prosent.`0_PROSENT`,
        gradering = Prosent.`0_PROSENT`,
        opplysningerMottatt = null,
    )
}
