package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class ArbeidsevneGrunnlag(
    val arbeidsevneId: Long,
    val behandlingId: BehandlingId,
    val vurderinger: List<Arbeidsevnevurdering>,
)
