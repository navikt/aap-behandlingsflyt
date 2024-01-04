package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.flyt.vilkår.Avslagsårsak
import no.nav.aap.behandlingsflyt.flyt.vilkår.Utfall
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkår
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsperiode
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.tidslinje.Tidslinje
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RettTilRegelTest {

    private val regel = RettTilRegel()

    @Test
    fun `skal lage tidslinje med alle relevante vilkår`() {
        val søknadsdato = LocalDate.now().minusDays(29)
        val periode = Periode(søknadsdato, søknadsdato.plusYears(3))
        val aldersVilkåret =
            Vilkår(Vilkårtype.ALDERSVILKÅRET, setOf(Vilkårsperiode(periode, Utfall.OPPFYLT, false, null, null)))
        val sykdomsVilkåret =
            Vilkår(Vilkårtype.SYKDOMSVILKÅRET, setOf(Vilkårsperiode(periode, Utfall.OPPFYLT, false, null, null)))
        val bistandVilkåret =
            Vilkår(Vilkårtype.BISTANDSVILKÅRET, setOf(Vilkårsperiode(periode, Utfall.OPPFYLT, false, null, null)))

        val input = UnderveisInput(
            førsteFastsatteDag = søknadsdato,
            listOf(aldersVilkåret, sykdomsVilkåret, bistandVilkåret),
            listOf(),
            listOf()
        )
        val grunnleggendeRettTidslinje = regel.vurder(input = input, Tidslinje())

        val segmenter = grunnleggendeRettTidslinje.segmenter()
        assertThat(segmenter).hasSize(1)
        assertThat(segmenter.first().verdi!!.harRett()).isTrue()
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
                        null
                    ),
                    Vilkårsperiode(
                        Periode(
                            søknadsdato.plusYears(3).minusMonths(4).plusDays(1),
                            søknadsdato.plusYears(3)
                        ), Utfall.IKKE_OPPFYLT, false, null, null, avslagsårsak = Avslagsårsak.BRUKER_OVER_67
                    )
                )
            )
        val sykdomsVilkåret =
            Vilkår(Vilkårtype.SYKDOMSVILKÅRET, setOf(Vilkårsperiode(periode, Utfall.OPPFYLT, false, null, null)))
        val bistandVilkåret =
            Vilkår(Vilkårtype.BISTANDSVILKÅRET, setOf(Vilkårsperiode(periode, Utfall.OPPFYLT, false, null, null)))

        val input = UnderveisInput(
            førsteFastsatteDag = søknadsdato,
            listOf(aldersVilkåret, sykdomsVilkåret, bistandVilkåret),
            listOf(),
            listOf()
        )
        val grunnleggendeRettTidslinje = regel.vurder(input = input, Tidslinje())

        val segmenter = grunnleggendeRettTidslinje.segmenter()
        assertThat(segmenter).hasSize(2)
        assertThat(segmenter.first().verdi!!.harRett()).isTrue()
        assertThat(segmenter.last().verdi!!.harRett()).isFalse()
    }
}