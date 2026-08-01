package no.nav.aap.sykdom

import java.time.LocalDate
import no.nav.aap.vilkårsresultat.Vilkårsvurderer
import no.nav.aap.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid

object Sykdomsvilkår : Vilkårsvurderer<SykdomsFaktagrunnlag> {
    override val vilkårtype: Vilkårtype = Vilkårtype.SYKDOMSVILKÅRET

    private val regelTidslinje = Tidslinje(
        listOf(
            Segment(
                Periode(LocalDate.of(2023, 1, 1), Tid.MAKS),
                SykdomsvilkårUtenVissVarighet // TODO: Finne en bedre variant her enn å instansiere opp alle sammen
            )
        )
    )

    override fun vurder(faktagrunnlag: SykdomsFaktagrunnlag): Tidslinje<Vilkårsvurdering> {
        val segment = regelTidslinje.segment(faktagrunnlag.kravDato)
            ?: error("Fant ikke regler for vurderingsdato ${faktagrunnlag.kravDato}")
        val regel = segment.verdi

        return regel.vurder(faktagrunnlag)
    }
}