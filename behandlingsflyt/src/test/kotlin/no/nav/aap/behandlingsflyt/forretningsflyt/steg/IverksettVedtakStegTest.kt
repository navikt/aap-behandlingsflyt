package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.GosysService
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurderingRepository
import no.nav.aap.behandlingsflyt.behandling.stansopphør.StansOpphørService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingService
import no.nav.aap.behandlingsflyt.behandling.vedtak.Vedtak
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakId
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.prosessering.GenererVilkårsvurderingOppsummeringJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

class IverksettVedtakStegTest {
    private val sakRepository = mockk<SakRepository>(relaxed = true)
    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val refusjonkravRepository = mockk<RefusjonkravRepository>(relaxed = true)
    private val utbetalingService = mockk<UtbetalingService>()
    private val vedtakService = mockk<VedtakService>()
    private val virkningstidspunktUtleder = mockk<VirkningstidspunktUtleder>(relaxed = true)
    private val trukketSøknadService = mockk<TrukketSøknadService>()
    private val avbrytRevurderingService = mockk<AvbrytRevurderingService>()
    private val gosysService = mockk<GosysService>(relaxed = true)
    private val flytJobbRepository = mockk<FlytJobbRepository>()
    private val mellomlagretVurderingRepository = mockk<MellomlagretVurderingRepository>(relaxed = true)
    private val resultatUtleder = mockk<ResultatUtleder>(relaxed = true)
    private val stansOpphørService = mockk<StansOpphørService>(relaxed = true)
    private val unleashGateway = mockk<UnleashGateway>()

    private fun kontekst(
        behandlingType: TypeBehandling,
        vurderingsbehov: Vurderingsbehov,
        vurderingType: VurderingType,
    ) = flytKontekstMedPerioder {
        sakId = SakId(Random.nextLong())
        behandlingId = BehandlingId(Random.nextLong())
        this.behandlingType = behandlingType
        this.vurderingType = vurderingType
        vurderingsbehovRelevanteForSteg = setOf(vurderingsbehov)
    }

    private fun utførOgHentJobbtyper(
        behandlingType: TypeBehandling,
        vurderingsbehov: Vurderingsbehov,
        vurderingType: VurderingType,
        stans: Boolean = false,
    ): List<String> {
        val kontekst = kontekst(behandlingType, vurderingsbehov, vurderingType)
        val jobber = mutableListOf<JobbInput>()
        val vedtak = Vedtak(
            id = VedtakId(1),
            behandlingId = kontekst.behandlingId,
            vedtakstidspunkt = LocalDateTime.now(),
            virkningstidspunkt = LocalDate.now(),
        )
        every { trukketSøknadService.søknadErTrukket(any()) } returns false
        every { avbrytRevurderingService.revurderingErAvbrutt(any()) } returns false
        every { vedtakService.hentVedtak(kontekst.behandlingId) } returns vedtak
        every {
            utbetalingService.lagTilkjentYtelseForUtbetaling(kontekst.sakId, kontekst.behandlingId)
        } returns null
        every {
            unleashGateway.isEnabled(BehandlingsflytFeature.GenererVilkarsvurderingOppsummeringPDF)
        } returns true
        every { flytJobbRepository.leggTil(capture(jobber)) } just runs
        if (stans) {
            every { stansOpphørService.vedtattStansOpphør(kontekst.behandlingId) } returns listOf(
                GjeldendeStansEllerOpphør(
                    fom = LocalDate.now(),
                    opprettet = Instant.now(),
                    vurdertIBehandling = kontekst.behandlingId,
                    vurdering = Stans(emptySet()),
                )
            )
        }

        IverksettVedtakSteg(
            sakRepository = sakRepository,
            behandlingRepository = behandlingRepository,
            refusjonkravRepository = refusjonkravRepository,
            utbetalingService = utbetalingService,
            vedtakService = vedtakService,
            virkningstidspunktUtleder = virkningstidspunktUtleder,
            trukketSøknadService = trukketSøknadService,
            avbrytRevurderingService = avbrytRevurderingService,
            gosysService = gosysService,
            flytJobbRepository = flytJobbRepository,
            mellomlagretVurderingRepository = mellomlagretVurderingRepository,
            resultatUtleder = resultatUtleder,
            stansOpphørService = stansOpphørService,
            unleashGateway = unleashGateway,
        ).utfør(kontekst)

        return jobber.map { it.type() }
    }

    @ParameterizedTest
    @EnumSource(
        Vurderingsbehov::class,
        mode = Mode.INCLUDE,
        names = ["VEDTAKSLENGDE_MANUELT", "VURDER_RETTIGHETSPERIODE", "VURDER_KRAV", "BARNETILLEGG"],
    )
    fun `oppretter jobb for valgte revurderinger`(vurderingsbehov: Vurderingsbehov) {
        val jobbtyper = utførOgHentJobbtyper(
            TypeBehandling.Revurdering,
            vurderingsbehov,
            VurderingType.REVURDERING,
        )

        assertThat(jobbtyper).contains(GenererVilkårsvurderingOppsummeringJobbUtfører.type)
    }

    @Test
    fun `oppretter jobb for førstegangsbehandling og nye stansvedtak`() {
        val førstegangsjobbtyper = utførOgHentJobbtyper(
            TypeBehandling.Førstegangsbehandling,
            Vurderingsbehov.MOTTATT_SØKNAD,
            VurderingType.FØRSTEGANGSBEHANDLING,
        )
        val stansjobbtyper = utførOgHentJobbtyper(
            TypeBehandling.Revurdering,
            Vurderingsbehov.OPPHOLDSKRAV,
            VurderingType.REVURDERING,
            stans = true,
        )

        assertThat(førstegangsjobbtyper).contains(GenererVilkårsvurderingOppsummeringJobbUtfører.type)
        assertThat(stansjobbtyper).contains(GenererVilkårsvurderingOppsummeringJobbUtfører.type)
    }

    @Test
    fun `oppretter ikke jobb for tekniske kjøringer`() {
        val jobbtyper = utførOgHentJobbtyper(
            TypeBehandling.Revurdering,
            Vurderingsbehov.OPPHOLDSKRAV,
            VurderingType.G_REGULERING,
        )

        assertThat(jobbtyper).doesNotContain(GenererVilkårsvurderingOppsummeringJobbUtfører.type)
    }
}
