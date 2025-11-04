package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

class Sykdomsvilkår(vilkårsresultat: Vilkårsresultat) :
    Vilkårsvurderer<SykdomsFaktagrunnlag> {
    private val regelTidslinje = Tidslinje(
        listOf(
            Segment(
                Periode(LocalDate.of(2023, 1, 1), Tid.MAKS),
                SykdomsvilkårFraLansering(vilkårsresultat) // TODO: Finne en bedre variant her enn å instansiere opp alle sammen
            )
        )
    )

    override fun vurder(grunnlag: SykdomsFaktagrunnlag) {
        val segment = regelTidslinje.segment(grunnlag.kravDato)
            ?: error("Fant ikke regler for vurderingsdato ${grunnlag.kravDato}")
        val regel = segment.verdi

        regel.vurder(grunnlag)
    }
}
