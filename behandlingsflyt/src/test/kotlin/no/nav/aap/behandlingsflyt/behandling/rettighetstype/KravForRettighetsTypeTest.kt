package no.nav.aap.behandlingsflyt.behandling.rettighetstype

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak.BRUKER_OVER_67
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak.VARIGHET_OVERSKREDET_OVERGANG_UFORE
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.ALDERSVILKÅRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.BISTANDSVILKÅRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.GRUNNLAGET
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.LOVVALG
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.MEDLEMSKAP
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.OVERGANGUFØREVILKÅRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.SYKDOMSVILKÅRET
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KravForRettighetsTypeTest {
    @Test
    fun `ordinær AAP-sak hvor medlemmet blir for gammel`() {
        val kravdato = 1 januar 2025
        val sisteDagAldersvilkåretOppfylt = 5 desember 2026
        val sisteVurdering = 17 desember 2026
        val rettighetsperiode = Periode(kravdato, sisteVurdering)

        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.vurdertOppfylt(LOVVALG, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(SYKDOMSVILKÅRET, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(BISTANDSVILKÅRET, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(MEDLEMSKAP, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(GRUNNLAGET, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(ALDERSVILKÅRET, Periode(kravdato, sisteDagAldersvilkåretOppfylt))
        vilkårsresultat.vurdertIkkeOppfylt(
            ALDERSVILKÅRET, Periode(sisteDagAldersvilkåretOppfylt.plusDays(1), sisteVurdering),
            BRUKER_OVER_67
        )

        assertTidslinje(
            vurderRettighetsType(vilkårsresultat),
            Periode(kravdato, sisteDagAldersvilkåretOppfylt) to {
                assertThat(it).isEqualTo(RettighetsType.BISTANDSBEHOV)
            },
        )

        assertTidslinje(
            avslagsårsakerVedTapAvRettPåAAP(vilkårsresultat),
            Periode(sisteDagAldersvilkåretOppfylt, sisteDagAldersvilkåretOppfylt) to {
                assertThat(it).isEqualTo(setOf(BRUKER_OVER_67))
            }
        )
    }

    @Test
    fun `overgang uføre hvor varighet overskides`() {
        /* NB: den faktiske varigheten knyttet til 11-18 regnes ikke ut her, men i vilkårsvurderingen
          * av OVERGANGUFØREVILKÅRET-vurderignen. Så sluttdatoen valgt
          * her er litt tilfeldig.
         */
        val kravdato = 12 januar 2025
        val sisteVurderingsdato = 11 januar 2026
        val sisteDagOvergangUføre = 5 desember 2025

        val rettighetsperiode = Periode(kravdato, sisteVurderingsdato)

        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.vurdertOppfylt(LOVVALG, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(SYKDOMSVILKÅRET, rettighetsperiode)
        vilkårsresultat.vurdertIkkeOppfylt(BISTANDSVILKÅRET, rettighetsperiode, IKKE_BEHOV_FOR_OPPFOLGING)
        vilkårsresultat.vurdertOppfylt(MEDLEMSKAP, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(GRUNNLAGET, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(ALDERSVILKÅRET, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(OVERGANGUFØREVILKÅRET, Periode(kravdato, sisteDagOvergangUføre))
        vilkårsresultat.vurdertIkkeOppfylt(
            OVERGANGUFØREVILKÅRET, Periode(sisteDagOvergangUføre.plusDays(1), sisteVurderingsdato),
            VARIGHET_OVERSKREDET_OVERGANG_UFORE
        )

        assertTidslinje(
            vurderRettighetsType(vilkårsresultat),
            Periode(kravdato, sisteDagOvergangUføre) to {
                assertThat(it).isEqualTo(RettighetsType.VURDERES_FOR_UFØRETRYGD)
            },
        )

        assertTidslinje(
            avslagsårsakerVedTapAvRettPåAAP(vilkårsresultat),
            Periode(sisteDagOvergangUføre, sisteDagOvergangUføre) to {
                /* Legg spesielt merke til at IKKE_BEHOV_FOR_OPPFOLGING
                 * (bistandsbehovet) ikke er med her, selv om det
                * vilkåret ikke er oppfylt. Det er fordi det vilkåret ikke var
                * årsaken til at brukeren gikk fra å ha rett til å ikke ha rett.*/
                assertThat(it).isEqualTo(setOf(VARIGHET_OVERSKREDET_OVERGANG_UFORE))
            }
        )
    }

    private fun Vilkårsresultat.vurdertOppfylt(
        vilkårtype: Vilkårtype,
        periode: Periode,
        innvilgelsesårsak: Innvilgelsesårsak? = null
    ) {
        this.leggTilHvisIkkeEksisterer(vilkårtype)
            .leggTilVurdering(
                Vilkårsperiode(
                    periode = periode,
                    vilkårsvurdering = Vilkårsvurdering(
                        utfall = Utfall.OPPFYLT,
                        innvilgelsesårsak = innvilgelsesårsak,
                        manuellVurdering = false,
                        begrunnelse = ".",
                        faktagrunnlag = null,
                    ),
                )
            )
    }

    private fun Vilkårsresultat.vurdertIkkeOppfylt(
        vilkårtype: Vilkårtype,
        periode: Periode,
        avslagsårsak: Avslagsårsak
    ) {
        this.leggTilHvisIkkeEksisterer(vilkårtype)
            .leggTilVurdering(
                Vilkårsperiode(
                    periode = periode,
                    vilkårsvurdering = Vilkårsvurdering(
                        utfall = Utfall.IKKE_OPPFYLT,
                        avslagsårsak = avslagsårsak,
                        manuellVurdering = false,
                        begrunnelse = ".",
                        faktagrunnlag = null,
                    ),
                )
            )
    }
}