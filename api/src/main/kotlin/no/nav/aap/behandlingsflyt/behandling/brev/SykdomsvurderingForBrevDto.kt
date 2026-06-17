package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse


data class SykdomsvurderingForBrevDto(
    val vurdering: SykdomsvurderingForBrevVurderingDto?,
    val historiskeVurderinger: List<SykdomsvurderingForBrevVurderingDto>,
    val kanSaksbehandle: Boolean
)

data class SykdomsvurderingForBrevVurderingDto(
    val vurdering: String?,
    val vurderingerMeta: VurderingerMetaResponse,
)
