package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

class UføreGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val vurderinger: List<Uføre>,
)
