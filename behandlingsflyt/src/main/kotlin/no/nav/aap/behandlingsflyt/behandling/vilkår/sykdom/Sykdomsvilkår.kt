package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

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
