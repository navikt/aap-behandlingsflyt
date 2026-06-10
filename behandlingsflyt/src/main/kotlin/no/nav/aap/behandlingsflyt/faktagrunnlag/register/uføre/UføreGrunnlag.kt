package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

data class UføreGrunnlag(
    val vurderinger: Set<Uføre>,
)
