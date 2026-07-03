package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakId
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.help.person
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status as BehandlingStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class MeldeperiodeTilMeldekortBackendJobbUtførerUtførTest {

    private val sakId = SakId(1L)
    private val behandlingId_1 = BehandlingId(1L)
    private val behandlingId_2 = BehandlingId(2L)

    private val sakService = mockk<SakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val meldekortGateway = mockk<MeldekortGateway>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val meldeperiodeRepository = mockk<MeldeperiodeRepository>()
    private val underveisRepository = mockk<UnderveisRepository>()
    private val meldepliktRepository = mockk<MeldepliktRepository>()
    private val trukketSøknadService = mockk<TrukketSøknadService>()
    private val vedtakService = mockk<VedtakService>()
    private val unleashGateway = mockk<UnleashGateway>()

    private val utfører = MeldeperiodeTilMeldekortBackendJobbUtfører(
        sakService, behandlingService, meldekortGateway, behandlingRepository,
        meldeperiodeRepository, underveisRepository, meldepliktRepository,
        trukketSøknadService, vedtakService, unleashGateway
    )

    private val sak = Sak(
        id = sakId,
        saksnummer = Saksnummer("123"),
        person = person(),
        rettighetsperiode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)),
    )

    private val behandling_1 = lagBehandling(behandlingId_1)
    private val behandling_2 = lagBehandling(behandlingId_2)

    private val underveisGrunnlagUtenRett = UnderveisGrunnlag(
        id = 1L,
        perioder = listOf(underveisperiodeUtenRett(Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1))))
    )
    private val underveisGrunnlagMedRett = UnderveisGrunnlag(
        id = 2L,
        perioder = listOf(underveisperiodeMedRett(Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1))))
    )

    init {
        // behandlingId_1 har ikke opplysningsbehov (brukes i asserts i testene)
        every { underveisRepository.hentHvisEksisterer(behandlingId_1) } returns underveisGrunnlagUtenRett
        // behandlingId_2 har opplysningsbehov (brukes i asserts i testene)
        every { underveisRepository.hentHvisEksisterer(behandlingId_2) } returns underveisGrunnlagMedRett

        every { sakService.hent(sakId) } returns sak
        every { behandlingRepository.hent(behandlingId_1) } returns behandling_1
        every { behandlingRepository.hent(behandlingId_2) } returns behandling_2
        every { trukketSøknadService.søknadErTrukket(any()) } returns false
        every { meldeperiodeRepository.hentMeldeperioder(any(), any()) } returns emptyList()
        every { meldepliktRepository.hentHvisEksisterer(any()) } returns null
        every { vedtakService.vedtakstidspunktFørsteInnvilgelse(any()) } returns null
        every { meldekortGateway.oppdaterMeldeperioder(any()) } returns Unit
    }

    @Test
    fun `feature toggle av - sender data basert på triggerende behandling`() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.MeldeperiodeTilMeldekortBackendBasertPaaSisteFattedeVedtak) } returns false

        val sendt = slot<MeldeperioderV0>()
        every { meldekortGateway.oppdaterMeldeperioder(capture(sendt)) } returns Unit

        utfører.utfør(nyJobb(behandlingId_1))

        assertThat(sendt.captured.opplysningsbehov).isEmpty()
    }

    @Test
    fun `feature toggle på - sender data basert på siste fattede vedtak når den er nyere enn behandlingen som trigget jobben`() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.MeldeperiodeTilMeldekortBackendBasertPaaSisteFattedeVedtak) } returns true
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns behandlingMedVedtak(behandlingId_2)

        val sendt = slot<MeldeperioderV0>()
        every { meldekortGateway.oppdaterMeldeperioder(capture(sendt)) } returns Unit

        utfører.utfør(nyJobb(behandlingId_1))

        assertThat(sendt.captured.opplysningsbehov).isNotEmpty()
    }

    @Test
    fun `feature toggle på - ingen siste fattede vedtak, faller tilbake til behandlingen som trigget jobben`() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.MeldeperiodeTilMeldekortBackendBasertPaaSisteFattedeVedtak) } returns true
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns null

        val sendt = slot<MeldeperioderV0>()
        every { meldekortGateway.oppdaterMeldeperioder(capture(sendt)) } returns Unit

        utfører.utfør(nyJobb(behandlingId_1))

        assertThat(sendt.captured.opplysningsbehov).isEmpty()
    }

    @Test
    fun `feature toggle på - siste fattede vedtak er samme som behandlingen som trigget jobben`() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.MeldeperiodeTilMeldekortBackendBasertPaaSisteFattedeVedtak) } returns true
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns behandlingMedVedtak(behandlingId_1)

        val sendt = slot<MeldeperioderV0>()
        every { meldekortGateway.oppdaterMeldeperioder(capture(sendt)) } returns Unit

        utfører.utfør(nyJobb(behandlingId_1))

        assertThat(sendt.captured.opplysningsbehov).isEmpty()
    }

    private fun nyJobb(behandlingId: BehandlingId) =
        MeldeperiodeTilMeldekortBackendJobbUtfører.nyJobb(sakId, behandlingId)

    private fun behandlingMedVedtak(behandlingId: BehandlingId) = BehandlingMedVedtak(
        saksnummer = Saksnummer("123"),
        id = behandlingId,
        referanse = BehandlingReferanse(),
        typeBehandling = TypeBehandling.Førstegangsbehandling,
        status = BehandlingStatus.AVSLUTTET,
        opprettetTidspunkt = LocalDateTime.now(),
        vedtakstidspunkt = LocalDateTime.now(),
        virkningstidspunkt = null,
        vurderingsbehov = setOf(),
        årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
        forrigeBehandlingId = null,
        vedtakId = VedtakId(0),
    )

    private fun lagBehandling(id: BehandlingId): Behandling {
        return Behandling(
            id = id,
            forrigeBehandlingId = null,
            sakId = sakId,
            typeBehandling = TypeBehandling.Revurdering,
            status = BehandlingStatus.IVERKSETTES,
            årsakTilOpprettelse = null,
            versjon = 0
        )
    }

    private fun underveisperiodeUtenRett(periode: Periode) = Underveisperiode(
        periode = periode,
        meldePeriode = periode,
        utfall = Utfall.IKKE_OPPFYLT,
        rettighetsType = null,
        avslagsårsak = UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT,
        grenseverdi = Prosent(60),
        institusjonsoppholdReduksjon = Prosent(0),
        arbeidsgradering = nullArbeidsgradering(),
        trekk = Dagsatser(0),
        brukerAvKvoter = emptySet(),
        meldepliktStatus = MeldepliktStatus.FØR_VEDTAK,
        meldepliktGradering = Prosent.`0_PROSENT`,
    )

    private fun underveisperiodeMedRett(periode: Periode) = Underveisperiode(
        periode = periode,
        meldePeriode = periode,
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
