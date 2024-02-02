package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BistandGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val vurdering: BistandVurdering,
)
