package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.Dokumentkilde


data class SykdomsvurderingForBrevDto(
    val vurdering: SykdomsvurderingForBrevVurderingDto?,
    val historiskeVurderinger: List<SykdomsvurderingForBrevVurderingDto>,
    val kanSaksbehandle: Boolean
)

data class SykdomsvurderingForBrevVurderingDto(
    val vurdering: String?,
    val vurderingerMeta: VurderingerMetaResponse,
    val kilder: List<Dokumentkilde> = emptyList(),
)
