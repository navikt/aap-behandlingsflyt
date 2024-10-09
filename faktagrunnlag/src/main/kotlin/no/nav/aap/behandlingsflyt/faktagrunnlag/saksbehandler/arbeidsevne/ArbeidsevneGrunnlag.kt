package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.verdityper.sakogbehandling.BehandlingId

data class ArbeidsevneGrunnlag(
    val arbeidsevneId: Long,
    val behandlingId: BehandlingId,
    val vurderinger: List<ArbeidsevneVurdering>,
)
