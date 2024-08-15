package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn

import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident

class BarnVurderingGrunnlag(
    val id: Long, val behandlingId: BehandlingId, val vurdering: BarnVurdering
) {
    fun tidslinje(): Tidslinje<Set<Ident>> {
        return Tidslinje(vurdering.barn.map { barnVurderingPeriode ->
            Segment(
                verdi = barnVurderingPeriode.barn, periode = barnVurderingPeriode.periode
            )
        })
    }
}