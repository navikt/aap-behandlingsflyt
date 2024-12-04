package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

class BistandGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val vurdering: BistandVurdering,
)
