package no.nav.aap.behandlingsflyt.behandling.foreslåvedtak

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UunngåeligAvslagTidslinjeTest {

    private fun dato(måned: Int, dag: Int) = LocalDate.of(2025, måned, dag)

    private fun vilkårsresultat(vararg perioder: Triple<Periode, Utfall, Avslagsårsak?>): Vilkårsresultat {
        val vilkår = Vilkår(
            type = Vilkårtype.ALDERSVILKÅRET,
            vilkårsperioder = perioder.map { (periode, utfall, årsak) ->
                Vilkårsperiode(
                    periode = periode,
                    utfall = utfall,
                    begrunnelse = null,
                    avslagsårsak = årsak,
                )
            }.toSet(),
        )
        return Vilkårsresultat(vilkår = listOf(vilkår))
    }

    private fun uunngåelig(periode: Periode) = Tidslinje(
        listOf(
            Segment(
                periode,
                TidligereVurderinger.UunngåeligAvslag(Vilkårtype.ALDERSVILKÅRET) as TidligereVurderinger.Behandlingsutfall
            )
        )
    )

    @Test
    fun `bruker avslagsårsak fra segmentet som overlapper avslagsperioden`() {
        val vilkår = vilkårsresultat(
            Triple(Periode(dato(1, 1), dato(3, 31)), Utfall.OPPFYLT, null),
            Triple(Periode(dato(4, 1), dato(12, 31)), Utfall.IKKE_OPPFYLT, Avslagsårsak.BRUKER_OVER_67),
        )

        val resultat = utledUunngåeligAvslagTidslinje(
            uunngåelig(Periode(dato(4, 1), dato(12, 31))),
            vilkår,
        )

        assertThat(resultat.segmenter().map { it.verdi.avslagsårsak })
            .containsExactly(Avslagsårsak.BRUKER_OVER_67)
    }

    @Test
    fun `ignorerer oppfylt segment før avslagsperioden`() {
        val vilkår = vilkårsresultat(
            Triple(Periode(dato(1, 1), dato(6, 30)), Utfall.OPPFYLT, null),
            Triple(Periode(dato(7, 1), dato(12, 31)), Utfall.IKKE_OPPFYLT, Avslagsårsak.BRUKER_UNDER_18),
        )

        val resultat = utledUunngåeligAvslagTidslinje(
            uunngåelig(Periode(dato(7, 1), dato(12, 31))),
            vilkår,
        )

        assertThat(resultat.segmenter().single().verdi.avslagsårsak).isEqualTo(Avslagsårsak.BRUKER_UNDER_18)
    }

    @Test
    fun `gir null avslagsårsak når vilkåret ikke har avslag i perioden`() {
        val vilkår = vilkårsresultat(
            Triple(Periode(dato(1, 1), dato(12, 31)), Utfall.OPPFYLT, null),
        )

        val resultat = utledUunngåeligAvslagTidslinje(
            uunngåelig(Periode(dato(4, 1), dato(12, 31))),
            vilkår,
        )

        assertThat(resultat.segmenter().single().verdi.avslagsårsak).isNull()
    }

    @Test
    fun `gir tom tidslinje når ingen perioder er uunngåelig avslag`() {
        val resultat = utledUunngåeligAvslagTidslinje(
            Tidslinje(
                listOf(
                    Segment(
                        Periode(dato(1, 1), dato(12, 31)),
                        TidligereVurderinger.PotensieltOppfylt(null) as TidligereVurderinger.Behandlingsutfall
                    )
                )
            ),
            vilkårsresultat(),
        )

        assertThat(resultat.segmenter()).isEmpty()
    }
}