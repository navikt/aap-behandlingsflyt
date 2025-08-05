package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse


data class SykdomsvurderingForBrevDto(
    val vurdering: SykdomsvurderingForBrevVurderingDto?,
    val historiskeVurderinger: List<SykdomsvurderingForBrevVurderingDto>,
    val kanSaksbehandle: Boolean
)

data class SykdomsvurderingForBrevVurderingDto(
    val vurdering: String?,
    val vurdertAv: VurdertAvResponse,
)
