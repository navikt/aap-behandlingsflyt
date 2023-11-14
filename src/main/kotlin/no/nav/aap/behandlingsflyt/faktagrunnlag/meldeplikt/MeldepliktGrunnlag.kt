package no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt

import no.nav.aap.behandlingsflyt.avklaringsbehov.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.behandling.BehandlingId

data class MeldepliktGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val vurderinger: List<Fritaksvurdering>
)
