package no.nav.aap.behandlingsflyt.faktagrunnlag.uføre

import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class UføreGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val vurdering: Uføre,
)
