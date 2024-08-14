package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg

import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident

class BarnVurderingGrunnlag(
    val vurdering: ManuelleBarnVurdeirng
) {
    fun tidslinje(): Tidslinje<Set<Ident>> {
        return Tidslinje(vurdering.barn.flatMap { barn ->
            barn.perioder.map { periode ->
                Segment(
                    verdi = setOf(barn.ident), periode = periode
                )
            }
        })
    }
}