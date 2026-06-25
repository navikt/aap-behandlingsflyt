package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningGradering
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.samordning.YtelseGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SamordningAvslagStegTest {

    val uføreRepository = mockk<UføreRepository>()
    val samordningUføreRepository = mockk<SamordningUføreRepository>()
    val samordningYtelseRepository = mockk<SamordningYtelseRepository>()
    val samordningVurderingRepository = mockk<SamordningVurderingRepository>()
    val avklaringsbehovRepository = mockk<AvklaringsbehovRepository>()
    val vilkårsresultatRepository = mockk<VilkårsresultatRepository>()
    val kontekst = mockk<FlytKontekstMedPerioder>(relaxed = true)

    val steg = SamordningAvslagSteg(
        uføreRepository = uføreRepository,
        samordningUføreRepository = samordningUføreRepository,
        samordningYtelseRepository = samordningYtelseRepository,
        samordningVurderingRepository = samordningVurderingRepository,
        vilkårsresultatRepository = vilkårsresultatRepository,
        tidligereVurderinger = FakeTidligereVurderinger(),
    )
    val periode = Periode(1 februar 2025, 31 mars 2025)
    val rettighetsperiode = Periode(1 januar 2025, 31 desember 2025)
    val forventetPeriode = Periode(periode.fom, rettighetsperiode.tom)

    @BeforeEach
    fun setUp() {
        every { kontekst.rettighetsperiode } returns rettighetsperiode
        every { vilkårsresultatRepository.hent(any()) } returns Vilkårsresultat(id = 1L, vilkår = emptyList())
        every { uføreRepository.hentHvisEksisterer(any()) } returns null
        every { samordningUføreRepository.hentHvisEksisterer(any()) } returns null
        every { samordningYtelseRepository.hentHvisEksisterer(any()) } returns null
        every { samordningVurderingRepository.hentHvisEksisterer(any()) } returns null
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
        val ytelseGrunnlag = SamordningYtelseGrunnlag(
            grunnlagId = 1L,
            ytelser = setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    kilde = "kilde",
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = periode,
                            gradering = Prosent.`100_PROSENT`,
                        )
                    )
                )
            )
        )
        val vurderingGrunnlag = SamordningVurderingGrunnlag(
            begrunnelse = "begrunnelse",
            vurderinger = setOf(
                SamordningVurdering(
                    ytelseType = Ytelse.SYKEPENGER,
                    vurderingPerioder = setOf(
                        SamordningVurderingPeriode(
                            periode = periode,
                            gradering = Prosent.`100_PROSENT`,
                            manuell = true,
                        )
                    )
                )
            ),
            vurdertAv = "test",
            vurdertTidspunkt = LocalDateTime.now()
        )

        every { samordningYtelseRepository.hentHvisEksisterer(any()) } returns ytelseGrunnlag
        every { samordningVurderingRepository.hentHvisEksisterer(any()) } returns vurderingGrunnlag

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