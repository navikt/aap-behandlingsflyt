package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Repository
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningGradering
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.samordning.YtelseGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
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

class SamordningAvslagStegTest {

    val samordningService = mockk<SamordningService>()
    val uføreRepository = mockk<UføreRepository>()
    val samordningUføreRepository = mockk<SamordningUføreRepository>()
    val avslag1127repository = mockk<Avslag11_27Repository>()
    val avklaringsbehovRepository = mockk<AvklaringsbehovRepository>()
    val vilkårsresultatRepository = mockk<VilkårsresultatRepository>()
    val kravRepository = mockk<KravRepository>()
    val kontekst = mockk<FlytKontekstMedPerioder>(relaxed = true)

    val steg = SamordningAvslagSteg(
        samordningService = samordningService,
        uføreRepository = uføreRepository,
        samordningUføreRepository = samordningUføreRepository,
        avslag1127repository = avslag1127repository,
        vilkårsresultatRepository = vilkårsresultatRepository,
        tidligereVurderinger = FakeTidligereVurderinger(),
        kravRepository = kravRepository,
    )
    val periode = Periode(1 februar 2025, 31 mars 2025)
    val rettighetsperiode = Periode(1 januar 2025, 31 desember 2025)
    val forventetPeriode = Periode(periode.fom, rettighetsperiode.tom)

    @BeforeEach
    fun setUp() {
        every { kontekst.rettighetsperiode } returns rettighetsperiode
        every { vilkårsresultatRepository.hent(any()) } returns Vilkårsresultat(id = 1L, vilkår = emptyList())
        every { samordningService.hentYtelser(any()) } returns null
        every { samordningService.hentVurderinger(any()) } returns null
        every { uføreRepository.hentHvisEksisterer(any()) } returns null
        every { samordningUføreRepository.hentHvisEksisterer(any()) } returns null
    }

    private fun samordningUføregrunnlag(uføregrad: Prosent): SamordningUføreGrunnlag {
        val samordningUføreVurderingPeriode = SamordningUføreVurderingPeriode(
            uføregradTilSamordning = uføregrad,
            virkningstidspunkt = periode.fom,
        )

        return SamordningUføreGrunnlag(
            vurdering = SamordningUføreVurdering(
                begrunnelse = "begrunnelse",
                vurderingPerioder = listOf(samordningUføreVurderingPeriode),
                vurdertAv = "test"
            )
        )
    }

    @Test
    fun `skal avslå hvis samordning ytelse er 100 i en periode`() {
        val samordningGradering = SamordningGradering(
            Prosent.`100_PROSENT`, listOf(
                YtelseGradering(Ytelse.SYKEPENGER, Prosent.`100_PROSENT`)
            )
        )
        every { samordningService.tidslinje(any()) } returns Tidslinje(periode, samordningGradering)
        var capturedVilkår: Vilkårsresultat? = null
        every { vilkårsresultatRepository.lagre(any(), any()) } answers {
            capturedVilkår = secondArg()
        }
        steg.utfør(kontekst = kontekst)
        val vilkårTidslinje = capturedVilkår!!.finnVilkår(Vilkårtype.SAMORDNING).tidslinje()
        assertThat(vilkårTidslinje.segmenter().count()).isEqualTo(1)
        assertThat(vilkårTidslinje.segmenter().first().verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        assertThat(vilkårTidslinje.segmenter().first().periode).isEqualTo(periode)
    }

    @Test
    fun `skal avslå hvis samordning uføre er 100 i en periode`() {
        every { samordningService.tidslinje(any()) } returns Tidslinje.empty()
        every { samordningUføreRepository.hentHvisEksisterer(any()) } returns samordningUføregrunnlag(Prosent.`100_PROSENT`)
        var capturedVilkår: Vilkårsresultat? = null
        every { vilkårsresultatRepository.lagre(any(), any()) } answers {
            capturedVilkår = secondArg()
        }
        steg.utfør(kontekst = kontekst)
        val vilkårTidslinje = capturedVilkår!!.finnVilkår(Vilkårtype.SAMORDNING).tidslinje()
        assertThat(vilkårTidslinje.segmenter().count()).isEqualTo(1)
        assertThat(vilkårTidslinje.segmenter().first().verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        assertThat(vilkårTidslinje.segmenter().first().periode).isEqualTo(forventetPeriode)
    }

    @Test
    fun `skal ikke avslå hvis samordning ytelse er 50 og samordnung uføre er 50`() {
        val samordningGradering = SamordningGradering(
            Prosent.`50_PROSENT`, listOf(
                YtelseGradering(Ytelse.SYKEPENGER, Prosent.`50_PROSENT`)
            )
        )
        every { samordningService.tidslinje(any()) } returns Tidslinje(forventetPeriode, samordningGradering)
        every { samordningUføreRepository.hentHvisEksisterer(any()) } returns samordningUføregrunnlag(Prosent.`50_PROSENT`)
        var capturedVilkår: Vilkårsresultat? = null
        every { vilkårsresultatRepository.lagre(any(), any()) } answers {
            capturedVilkår = secondArg()
        }
        steg.utfør(kontekst = kontekst)
        val vilkårTidslinje = capturedVilkår!!.finnVilkår(Vilkårtype.SAMORDNING).tidslinje()
        assertThat(vilkårTidslinje.segmenter().count()).isEqualTo(1)
        assertThat(vilkårTidslinje.segmenter().first().verdi.utfall).isEqualTo(Utfall.IKKE_VURDERT)
        assertThat(vilkårTidslinje.segmenter().first().periode).isEqualTo(forventetPeriode)
    }

}