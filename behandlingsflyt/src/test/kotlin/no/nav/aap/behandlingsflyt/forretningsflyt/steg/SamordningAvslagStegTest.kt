package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.Runs
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningGradering
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.samordning.YtelseGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreService
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@MockKExtension.CheckUnnecessaryStub
class SamordningAvslagStegTest {

    val samordningService = mockk<SamordningService>()
    val uføreService = mockk<UføreService>()
    val avklaringsbehovRepository = mockk<AvklaringsbehovRepository>()
    val vilkårsresultatRepository = mockk<VilkårsresultatRepository>()
    val kontekst = mockk<FlytKontekstMedPerioder>(relaxed = true)

    val steg = SamordningAvslagSteg(
        samordningService = samordningService,
        uføreService = uføreService,
        vilkårsresultatRepository = vilkårsresultatRepository,
        tidligereVurderinger = FakeTidligereVurderinger(),
    )
    val periode = Periode(1 februar 2025, 31 mars 2025)
    val rettighetsperiode = Periode(1 januar 2025, 31 desember 2025)

    @BeforeEach
    fun setUp() {
        every { kontekst.rettighetsperiode } returns rettighetsperiode
        every { vilkårsresultatRepository.hent(any()) } returns Vilkårsresultat(id = 1L, vilkår = emptyList())
        every { samordningService.hentYtelser(any()) } returns null
        every { samordningService.hentVurderinger(any()) } returns null
        every { uføreService.hentRegisterGrunnlagHvisEksisterer(any()) } returns null
        every { uføreService.hentVurderingGrunnlagHvisEksisterer(any()) } returns null
    }

    @Test
    fun `skal avslå hvis samordning ytelse er 100 i en periode`() {
        val samordningGradering = SamordningGradering(
            Prosent.`100_PROSENT`, listOf(
                YtelseGradering(Ytelse.SYKEPENGER, Prosent.`100_PROSENT`)
            )
        )
        every { samordningService.tidslinje(any()) } returns Tidslinje(periode, samordningGradering)
        every { uføreService.tidslinje(any()) } returns Tidslinje.empty()
        val vilkårSlot = slot<Vilkårsresultat>()
        every { vilkårsresultatRepository.lagre(any(), capture(vilkårSlot)) } just Runs
        steg.utfør(kontekst = kontekst)
        val vilkårTidslinje = vilkårSlot.captured.finnVilkår(Vilkårtype.SAMORDNING).tidslinje()
        assertThat(vilkårTidslinje.segmenter().count()).isEqualTo(1)
        assertThat(vilkårTidslinje.segmenter().first().verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        assertThat(vilkårTidslinje.segmenter().first().periode).isEqualTo(periode)
    }

    @Test
    fun `skal avslå hvis samordning uføre er 100 i en periode`() {
        every { samordningService.tidslinje(any()) } returns Tidslinje.empty()
        every { uføreService.tidslinje(any()) } returns Tidslinje(periode, Prosent.`100_PROSENT`)
        val vilkårSlot = slot<Vilkårsresultat>()
        every { vilkårsresultatRepository.lagre(any(), capture(vilkårSlot)) } just Runs
        steg.utfør(kontekst = kontekst)
        val vilkårTidslinje = vilkårSlot.captured.finnVilkår(Vilkårtype.SAMORDNING).tidslinje()
        assertThat(vilkårTidslinje.segmenter().count()).isEqualTo(1)
        assertThat(vilkårTidslinje.segmenter().first().verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        assertThat(vilkårTidslinje.segmenter().first().periode).isEqualTo(periode)
    }

    @Test
    fun `skal ikke avslå hvis samordning ytelse er 50 og samordnung uføre er 50`() {
        val samordningGradering = SamordningGradering(
            Prosent.`50_PROSENT`, listOf(
                YtelseGradering(Ytelse.SYKEPENGER, Prosent.`50_PROSENT`)
            )
        )
        every { samordningService.tidslinje(any()) } returns Tidslinje(periode, samordningGradering)
        every { uføreService.tidslinje(any()) } returns Tidslinje(periode, Prosent.`50_PROSENT`)
        val vilkårSlot = slot<Vilkårsresultat>()
        every { vilkårsresultatRepository.lagre(any(), capture(vilkårSlot)) } just Runs
        steg.utfør(kontekst = kontekst)
        val vilkårTidslinje = vilkårSlot.captured.finnVilkår(Vilkårtype.SAMORDNING).tidslinje()
        assertThat(vilkårTidslinje.segmenter().count()).isEqualTo(1)
        assertThat(vilkårTidslinje.segmenter().first().verdi.utfall).isEqualTo(Utfall.IKKE_VURDERT)
        assertThat(vilkårTidslinje.segmenter().first().periode).isEqualTo(periode)
    }

}