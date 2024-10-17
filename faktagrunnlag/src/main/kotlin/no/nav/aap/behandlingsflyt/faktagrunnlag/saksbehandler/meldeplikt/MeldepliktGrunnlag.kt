package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.verdityper.sakogbehandling.BehandlingId

data class MeldepliktGrunnlag(
    val meldepliktId: Long,
    val behandlingId: BehandlingId,
    val vurderinger: List<Fritaksvurdering>
)