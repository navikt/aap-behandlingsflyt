package no.nav.aap.behandlingsflyt.behandling.vedtakslengde

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetstypeService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class VedtakslengdeServiceTest {

    private val forrigeBehandlingId = BehandlingId(1L)
    private val behandlingId = BehandlingId(2L)
    private val dagensDato = 1 mars 2026
    private val clock = fixedClock(dagensDato)

    private val vedtakslengdeRepository = mockk<VedtakslengdeRepository>()
    private val underveisRepository = mockk<UnderveisRepository>()
    private val vilkårsresultatRepository = mockk<VilkårsresultatRepository>()
    private val rettighetstypeService = mockk<RettighetstypeService>()
    private val stansOpphørRepository = mockk<StansOpphørRepository>()

    @Test
    fun `returnerer IngenFremtidigBistandsbehovRettighet når det ikke finnes fremtidige perioder med rettighet`() {
        val vedtattSluttdato = 1 desember 2025

        setupForrigeBehandling(vedtattSluttdato)
        setupRettighetstypeTidslinje(emptyList())

        val resultat = vedtakslengdeService().hentNesteVedtakslengdeUtvidelse(
            behandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandlingId
        )

        assertThat(resultat).isEqualTo(VedtakslengdeUtvidelse.IngenFremtidigBistandsbehovRettighet)
    }

    @Test
    fun `returnerer IngenFremtidigBistandsbehovRettighet når rettighetstyper kun inneholder ikke-bistandsbehov typer`() {
        val vedtattSluttdato = 1 desember 2025

        setupForrigeBehandling(vedtattSluttdato)
        // SYKEPENGEERSTATNING er ikke BISTANDSBEHOV
        setupRettighetstypeTidslinje(
            listOf(Segment(Periode(1 januar 2026, 1 juni 2027), RettighetsType.SYKEPENGEERSTATNING))
        )

        val resultat = vedtakslengdeService().hentNesteVedtakslengdeUtvidelse(
            behandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandlingId
        )

        assertThat(resultat).isEqualTo(VedtakslengdeUtvidelse.IngenFremtidigBistandsbehovRettighet)
    }

    @Test
    fun `returnerer Automatisk med ett helt år når sammenhengende bistandsbehov rettighet er over ett år frem`() {
        val vedtattSluttdato = 1 desember 2025

        setupForrigeBehandling(vedtattSluttdato)
        // BISTANDSBEHOV langt frem i tid
        setupRettighetstypeTidslinje(
            listOf(Segment(Periode(1 januar 2025, Tid.MAKS), RettighetsType.BISTANDSBEHOV))
        )

        val resultat = vedtakslengdeService().hentNesteVedtakslengdeUtvidelse(
            behandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandlingId
        )

        assertThat(resultat).isInstanceOf(VedtakslengdeUtvidelse.Automatisk::class.java)
        val automatisk = resultat as VedtakslengdeUtvidelse.Automatisk
        assertThat(automatisk.forrigeSluttdato).isEqualTo(vedtattSluttdato)
        assertThat(automatisk.nySluttdato).isAfter(vedtattSluttdato)
        assertThat(automatisk.avslagsårsaker).isEmpty()
    }

    @Test
    fun `returnerer Manuell når det finnes flere ikke-sammenhengende bistandsbehov rettighetsperioder`() {
        val vedtattSluttdato = 1 desember 2025

        setupForrigeBehandling(vedtattSluttdato)
        // To separate perioder med bistandsbehov rettighet
        setupRettighetstypeTidslinje(
            listOf(
                Segment(Periode(2 desember 2025, 1 mars 2026), RettighetsType.BISTANDSBEHOV),
                Segment(Periode(2 mars 2026, 1 juni 2026), RettighetsType.SYKEPENGEERSTATNING),
                Segment(Periode(2 juni 2026, 1 desember 2026), RettighetsType.BISTANDSBEHOV),
            )
        )

        val resultat = vedtakslengdeService().hentNesteVedtakslengdeUtvidelse(
            behandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandlingId
        )

        assertThat(resultat).isInstanceOf(VedtakslengdeUtvidelse.Manuell::class.java)
        val manuell = resultat as VedtakslengdeUtvidelse.Manuell
        assertThat(manuell.forrigeSluttdato).isEqualTo(vedtattSluttdato)
        assertThat(manuell.avslagsårsaker).isEmpty()
    }

    @Test
    fun `returnerer Manuell når sammenhengende bistandsbehov rettighet er under ett år og toggle er av`() {
        val vedtattSluttdato = 1 desember 2025
        val bistandsbehovRettighetTom = 1 juni 2026

        setupForrigeBehandling(vedtattSluttdato)
        setupRettighetstypeTidslinje(
            listOf(Segment(Periode(1 januar 2025, bistandsbehovRettighetTom), RettighetsType.BISTANDSBEHOV))
        )
        setupStansOpphør(behandlingId, bistandsbehovRettighetTom.plusDays(1), setOf(Avslagsårsak.BRUKER_OVER_67))

        val resultat = vedtakslengdeService(unleash = AlleAvskruddUnleash).hentNesteVedtakslengdeUtvidelse(
            behandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandlingId
        )

        assertThat(resultat).isInstanceOf(VedtakslengdeUtvidelse.Manuell::class.java)
        val manuell = resultat as VedtakslengdeUtvidelse.Manuell
        assertThat(manuell.forrigeSluttdato).isEqualTo(vedtattSluttdato)
        assertThat(manuell.avslagsårsaker).containsExactly(Avslagsårsak.BRUKER_OVER_67)
    }

    @Test
    fun `returnerer Automatisk når sammenhengende bistandsbehov rettighet er under ett år med gyldig avslagsårsak og toggle er på`() {
        val vedtattSluttdato = 1 desember 2025
        val bistandsbehovRettighetTom = 1 juni 2026

        setupForrigeBehandling(vedtattSluttdato)
        setupRettighetstypeTidslinje(
            listOf(Segment(Periode(1 januar 2025, bistandsbehovRettighetTom), RettighetsType.BISTANDSBEHOV))
        )
        setupStansOpphør(behandlingId, bistandsbehovRettighetTom.plusDays(1), setOf(Avslagsårsak.BRUKER_OVER_67))

        val unleashMedToggle = FakeUnleashBaseWithDefaultDisabled(
            enabledFlags = listOf(BehandlingsflytFeature.UtvidVedtakslengdeUnderEttAr)
        )

        val resultat = vedtakslengdeService(unleash = unleashMedToggle).hentNesteVedtakslengdeUtvidelse(
            behandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandlingId
        )

        assertThat(resultat).isInstanceOf(VedtakslengdeUtvidelse.Automatisk::class.java)
        val automatisk = resultat as VedtakslengdeUtvidelse.Automatisk
        assertThat(automatisk.forrigeSluttdato).isEqualTo(vedtattSluttdato)
        assertThat(automatisk.nySluttdato).isEqualTo(bistandsbehovRettighetTom)
        assertThat(automatisk.avslagsårsaker).containsExactly(Avslagsårsak.BRUKER_OVER_67)
    }

    @Test
    fun `returnerer Manuell når sammenhengende bistandsbehov rettighet er under ett år med ugyldig avslagsårsak selv om toggle er på`() {
        val vedtattSluttdato = 1 desember 2025
        val bistandsbehovRettighetTom = 1 juni 2026

        setupForrigeBehandling(vedtattSluttdato)
        setupRettighetstypeTidslinje(
            listOf(Segment(Periode(1 januar 2025, bistandsbehovRettighetTom), RettighetsType.BISTANDSBEHOV))
        )
        // IKKE_BEHOV_FOR_OPPFOLGING er ikke blant de gyldige automatiske avslagsårsakene
        setupStansOpphør(behandlingId, bistandsbehovRettighetTom.plusDays(1), setOf(Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING))

        val unleashMedToggle = FakeUnleashBaseWithDefaultDisabled(
            enabledFlags = listOf(BehandlingsflytFeature.UtvidVedtakslengdeUnderEttAr)
        )

        val resultat = vedtakslengdeService(unleash = unleashMedToggle).hentNesteVedtakslengdeUtvidelse(
            behandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandlingId
        )

        assertThat(resultat).isInstanceOf(VedtakslengdeUtvidelse.Manuell::class.java)
        val manuell = resultat as VedtakslengdeUtvidelse.Manuell
        assertThat(manuell.avslagsårsaker).containsExactly(Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING)
    }

    @Test
    fun `returnerer Manuell når sammenhengende bistandsbehov rettighet er under ett år uten avslagsårsaker selv om toggle er på`() {
        val vedtattSluttdato = 1 desember 2025
        val bistandsbehovRettighetTom = 1 juni 2026

        setupForrigeBehandling(vedtattSluttdato)
        setupRettighetstypeTidslinje(
            listOf(Segment(Periode(1 januar 2025, bistandsbehovRettighetTom), RettighetsType.BISTANDSBEHOV))
        )
        every { stansOpphørRepository.hentHvisEksisterer(behandlingId) } returns null

        val unleashMedToggle = FakeUnleashBaseWithDefaultDisabled(
            enabledFlags = listOf(BehandlingsflytFeature.UtvidVedtakslengdeUnderEttAr)
        )

        val resultat = vedtakslengdeService(unleash = unleashMedToggle).hentNesteVedtakslengdeUtvidelse(
            behandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandlingId
        )

        assertThat(resultat).isInstanceOf(VedtakslengdeUtvidelse.Manuell::class.java)
        val manuell = resultat as VedtakslengdeUtvidelse.Manuell
        assertThat(manuell.avslagsårsaker).isEmpty()
    }

    @Test
    fun `bruker ANDRE_ÅR som utvidelse når forrige vedtakslengde hadde FØRSTE_ÅR`() {
        val vedtattSluttdato = 1 desember 2025

        setupForrigeBehandlingMedVedtakslengde(vedtattSluttdato, ÅrMedHverdager.FØRSTE_ÅR)
        setupRettighetstypeTidslinje(
            listOf(Segment(Periode(1 januar 2025, Tid.MAKS), RettighetsType.BISTANDSBEHOV))
        )

        val resultat = vedtakslengdeService().hentNesteVedtakslengdeUtvidelse(
            behandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandlingId
        )

        assertThat(resultat).isInstanceOf(VedtakslengdeUtvidelse.Automatisk::class.java)
        val automatisk = resultat as VedtakslengdeUtvidelse.Automatisk
        // ANDRE_ÅR har 261 hverdager, ny sluttdato er vedtattSluttdato + 261 hverdager
        assertThat(automatisk.nySluttdato).isEqualTo(vedtattSluttdato.plussHverdager(261))
    }

    @Test
    fun `bruker TREDJE_ÅR som utvidelse når forrige vedtakslengde hadde ANDRE_ÅR`() {
        val vedtattSluttdato = 1 desember 2025

        setupForrigeBehandlingMedVedtakslengde(vedtattSluttdato, ÅrMedHverdager.ANDRE_ÅR)
        setupRettighetstypeTidslinje(
            listOf(Segment(Periode(1 januar 2025, Tid.MAKS), RettighetsType.BISTANDSBEHOV))
        )

        val resultat = vedtakslengdeService().hentNesteVedtakslengdeUtvidelse(
            behandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandlingId
        )

        assertThat(resultat).isInstanceOf(VedtakslengdeUtvidelse.Automatisk::class.java)
        val automatisk = resultat as VedtakslengdeUtvidelse.Automatisk
        // TREDJE_ÅR har 262 hverdager
        assertThat(automatisk.nySluttdato).isEqualTo(vedtattSluttdato.plussHverdager(262))
    }

    @Test
    fun `bruker vedtakslengdevurdering sluttdato når den er senere enn underveisperiode sluttdato`() {
        val underveisSluttdato = 1 desember 2025
        val vedtakslengdeSluttdato = 15 desember 2025

        every { vedtakslengdeRepository.hentHvisEksisterer(forrigeBehandlingId) } returns VedtakslengdeGrunnlag(
            listOf(vedtakslengdeVurdering(vedtakslengdeSluttdato))
        )
        every { underveisRepository.hentHvisEksisterer(forrigeBehandlingId) } returns UnderveisGrunnlag(
            id = 1L,
            perioder = listOf(mockk { every { periode } returns Periode(1 januar 2025, underveisSluttdato) })
        )
        setupRettighetstypeTidslinje(
            listOf(Segment(Periode(1 januar 2025, Tid.MAKS), RettighetsType.BISTANDSBEHOV))
        )

        val resultat = vedtakslengdeService().hentNesteVedtakslengdeUtvidelse(
            behandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandlingId
        )

        assertThat(resultat).isInstanceOf(VedtakslengdeUtvidelse.Automatisk::class.java)
        val automatisk = resultat as VedtakslengdeUtvidelse.Automatisk
        assertThat(automatisk.forrigeSluttdato).isEqualTo(vedtakslengdeSluttdato)
    }

    @Test
    fun `returnerer Manuell når sammenhengende bistandsbehov rettighet ikke starter fra fraDato`() {
        val vedtattSluttdato = 1 desember 2025

        setupForrigeBehandling(vedtattSluttdato)
        setupRettighetstypeTidslinje(
            listOf(
                Segment(Periode(1 januar 2025, 1 desember 2025), RettighetsType.BISTANDSBEHOV),
                Segment(Periode(1 januar 2026, Tid.MAKS), RettighetsType.BISTANDSBEHOV)
            )
        )

        val resultat = vedtakslengdeService().hentNesteVedtakslengdeUtvidelse(
            behandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandlingId
        )

        assertThat(resultat).isInstanceOf(VedtakslengdeUtvidelse.Manuell::class.java)
        val manuell = resultat as VedtakslengdeUtvidelse.Manuell
        assertThat(manuell.forrigeSluttdato).isEqualTo(vedtattSluttdato)
    }

    @Test
    fun `returnerer IngenFremtidigBistandsbehovRettighet med ARBEIDSSØKER som har null kvote`() {
        val vedtattSluttdato = 1 desember 2025

        setupForrigeBehandling(vedtattSluttdato)
        // ARBEIDSSØKER er ikke BISTANDSBEHOV, filtreres bort
        setupRettighetstypeTidslinje(
            listOf(Segment(Periode(2 desember 2025, Tid.MAKS), RettighetsType.ARBEIDSSØKER))
        )

        val resultat = vedtakslengdeService().hentNesteVedtakslengdeUtvidelse(
            behandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandlingId
        )

        assertThat(resultat).isEqualTo(VedtakslengdeUtvidelse.IngenFremtidigBistandsbehovRettighet)
    }

    // -- Hjelpefunksjoner --

    private fun vedtakslengdeService(unleash: no.nav.aap.behandlingsflyt.unleash.UnleashGateway = AlleAvskruddUnleash) =
        VedtakslengdeService(
            vedtakslengdeRepository = vedtakslengdeRepository,
            underveisRepository = underveisRepository,
            vilkårsresultatRepository = vilkårsresultatRepository,
            rettighetstypeService = rettighetstypeService,
            stansOpphørRepository = stansOpphørRepository,
            unleashGateway = unleash,
            clock = clock,
        )

    /**
     * Setter opp forrige behandling uten vedtakslengdegrunnlag, med en underveisperiode
     * som gir [vedtattSluttdato] som siste sluttdato.
     */
    private fun setupForrigeBehandling(vedtattSluttdato: LocalDate) {
        every { vedtakslengdeRepository.hentHvisEksisterer(forrigeBehandlingId) } returns null
        every { underveisRepository.hentHvisEksisterer(forrigeBehandlingId) } returns UnderveisGrunnlag(
            id = 1L,
            perioder = listOf(mockk { every { periode } returns Periode(1 januar 2025, vedtattSluttdato) })
        )
    }

    /**
     * Setter opp forrige behandling med en vedtakslengdevurdering, slik at vi kan teste
     * progresjon av ÅrMedHverdager (FØRSTE_ÅR → ANDRE_ÅR → TREDJE_ÅR).
     */
    private fun setupForrigeBehandlingMedVedtakslengde(vedtattSluttdato: LocalDate, utvidetMed: ÅrMedHverdager) {
        every { vedtakslengdeRepository.hentHvisEksisterer(forrigeBehandlingId) } returns VedtakslengdeGrunnlag(
            listOf(vedtakslengdeVurdering(vedtattSluttdato, utvidetMed))
        )
        every { underveisRepository.hentHvisEksisterer(forrigeBehandlingId) } returns UnderveisGrunnlag(
            id = 1L,
            perioder = listOf(mockk { every { periode } returns Periode(1 januar 2025, vedtattSluttdato) })
        )
    }

    private fun setupRettighetstypeTidslinje(segmenter: List<Segment<RettighetsType>>) {
        every { rettighetstypeService.rettighetstypeTidslinjeBakoverkompatibel(behandlingId) } returns Tidslinje(segmenter)
    }

    private fun setupStansOpphør(behandlingId: BehandlingId, fom: LocalDate, årsaker: Set<Avslagsårsak>) {
        every { stansOpphørRepository.hentHvisEksisterer(behandlingId) } returns StansOpphørGrunnlag(
            setOf(
                GjeldendeStansEllerOpphør(
                    fom = fom,
                    opprettet = Instant.now(clock),
                    vurdertIBehandling = forrigeBehandlingId,
                    vurdering = Opphør(årsaker),
                )
            )
        )
    }

    private fun vedtakslengdeVurdering(
        sluttdato: LocalDate,
        utvidetMed: ÅrMedHverdager = ÅrMedHverdager.FØRSTE_ÅR,
    ) = VedtakslengdeVurdering(
        sluttdato = sluttdato,
        utvidetMed = utvidetMed,
        vurdertAv = SYSTEMBRUKER,
        vurdertIBehandling = forrigeBehandlingId,
        opprettet = Instant.now(clock),
        begrunnelse = "Automatisk vurdert"
    )

    /**
     * Hjelpefunksjon for å beregne forventet dato etter N hverdager fra en gitt dato.
     * Brukes til å verifisere at ÅrMedHverdager-beregningen gir forventet resultat.
     */
    private fun LocalDate.plussHverdager(antall: Int): LocalDate {
        var dag = this
        var teller = 0
        while (teller < antall) {
            dag = dag.plusDays(1)
            if (dag.dayOfWeek.value <= 5) {
                teller++
            }
        }
        return dag
    }
}



