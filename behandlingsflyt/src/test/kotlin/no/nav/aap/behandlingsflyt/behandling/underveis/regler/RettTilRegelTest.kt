package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RettTilRegelTest {

    @Test
    fun `skal lage tidslinje med alle relevante vilkår`() {
        val søknadsdato = LocalDate.now().minusDays(29)
        val periode = Periode(søknadsdato, søknadsdato.plusYears(3))
        val aldersVilkåret =
            Vilkår(
                Vilkårtype.ALDERSVILKÅRET, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val sykdomsVilkåret =
            Vilkår(
                Vilkårtype.SYKDOMSVILKÅRET, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val lovvalgsVilkåret =
            Vilkår(
                Vilkårtype.LOVVALG, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val medlemskapVilkåret =
            Vilkår(
                Vilkårtype.MEDLEMSKAP, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val bistandVilkåret =
            Vilkår(
                Vilkårtype.BISTANDSVILKÅRET, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val grunnlagVilkåret = Vilkår(
            Vilkårtype.GRUNNLAGET, setOf(
                Vilkårsperiode(
                    periode,
                    Utfall.OPPFYLT,
                    false,
                    null,
                    faktagrunnlag = null
                )
            )
        )

        val vilkårsresultat = Vilkårsresultat(
            vilkår = listOf(aldersVilkåret, lovvalgsVilkåret, sykdomsVilkåret, medlemskapVilkåret, bistandVilkåret, grunnlagVilkåret)
        ).rettighetstypeTidslinje()

        assertTidslinje(
            vilkårsresultat,
            periode to {
                assertThat(it).isEqualTo(RettighetsType.BISTANDSBEHOV)
            }
        )
    }

    @Test
    fun `skal lage tidslinje med alle relevante vilkår, men knekke ved avslag`() {
        val søknadsdato = LocalDate.now().minusDays(29)
        val periode1 = Periode(søknadsdato, søknadsdato.plusYears(3).minusMonths(4))
        val periode2 = Periode(periode1.tom.plusDays(1), søknadsdato.plusYears(3))
        val periode = Periode(periode1.fom, periode2.tom)
        val aldersVilkåret =
            Vilkår(
                Vilkårtype.ALDERSVILKÅRET, setOf(
                    Vilkårsperiode(
                        periode1,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    ),
                    Vilkårsperiode(
                        periode2,
                        Utfall.IKKE_OPPFYLT,
                        false,
                        null,
                        avslagsårsak = Avslagsårsak.BRUKER_OVER_67,
                        faktagrunnlag = null
                    )
                )
            )
        val sykdomsVilkåret =
            Vilkår(
                Vilkårtype.SYKDOMSVILKÅRET, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val lovvalgsVilkåret =
            Vilkår(
                Vilkårtype.LOVVALG, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val medlemskapVilkåret =
            Vilkår(
                Vilkårtype.MEDLEMSKAP, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val bistandVilkåret =
            Vilkår(
                Vilkårtype.BISTANDSVILKÅRET, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val grunnlagVilkåret = Vilkår(
            Vilkårtype.GRUNNLAGET, setOf(
                Vilkårsperiode(
                    periode,
                    Utfall.OPPFYLT,
                    false,
                    null,
                    faktagrunnlag = null
                )
            )
        )

        val vilkårsresultat = Vilkårsresultat(
            vilkår = listOf(
                aldersVilkåret,
                lovvalgsVilkåret,
                sykdomsVilkåret,
                medlemskapVilkåret,
                bistandVilkåret,
                grunnlagVilkåret,
            )
        ).rettighetstypeTidslinje()

        assertTidslinje(
            vilkårsresultat,
            periode1 to {
                assertThat(it).isEqualTo(RettighetsType.BISTANDSBEHOV)
            },
        )
    }
}