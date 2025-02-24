package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.tomKvoter
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RettTilRegelTest {

    private val regel = RettTilRegel()
    private val kvoter = tomKvoter

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
        /*val lovvalgsVilkåret =
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
            )*/
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

        val input = tomUnderveisInput.copy(
            rettighetsperiode = periode,
            vilkårsresultat = Vilkårsresultat(
                null,
                listOf(aldersVilkåret, /*lovvalgsVilkåret,*/ sykdomsVilkåret, medlemskapVilkåret, bistandVilkåret)
            ),
            kvoter = kvoter,
        )
        val grunnleggendeRettTidslinje = regel.vurder(input = input, Tidslinje())

        val segmenter = grunnleggendeRettTidslinje.segmenter()
        assertThat(segmenter).hasSize(1)
        assertThat(segmenter.first().verdi.ingenVilkårErAvslått()).isTrue()
    }

    @Test
    fun `skal lage tidslinje med alle relevante vilkår, men knekke ved avslag`() {
        val søknadsdato = LocalDate.now().minusDays(29)
        val periode = Periode(søknadsdato, søknadsdato.plusYears(3))
        val aldersVilkåret =
            Vilkår(
                Vilkårtype.ALDERSVILKÅRET, setOf(
                    Vilkårsperiode(
                        Periode(søknadsdato, søknadsdato.plusYears(3).minusMonths(4)),
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    ),
                    Vilkårsperiode(
                        Periode(
                            søknadsdato.plusYears(3).minusMonths(4).plusDays(1),
                            søknadsdato.plusYears(3)
                        ),
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
            )/*
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
            */
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

        val input = tomUnderveisInput.copy(
            rettighetsperiode = periode,
            vilkårsresultat = Vilkårsresultat(
                vilkår = listOf(
                    aldersVilkåret,
                    /*lovvalgsVilkåret,*/
                    sykdomsVilkåret,
                    medlemskapVilkåret,
                    bistandVilkåret
                )
            ),
            kvoter = kvoter,
        )
        val grunnleggendeRettTidslinje = regel.vurder(input = input, Tidslinje())

        val segmenter = grunnleggendeRettTidslinje.segmenter()
        assertThat(segmenter).hasSize(2)
        assertThat(segmenter.first().verdi.ingenVilkårErAvslått()).isTrue()
        assertThat(segmenter.last().verdi.ingenVilkårErAvslått()).isFalse()
    }
}