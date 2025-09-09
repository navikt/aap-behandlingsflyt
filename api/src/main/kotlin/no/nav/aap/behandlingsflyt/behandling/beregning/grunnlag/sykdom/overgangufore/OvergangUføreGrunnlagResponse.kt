package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangufore

import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom.SykdomsvurderingResponse

data class OvergangUføreGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: OvergangUføreVurderingResponse?,
    val gjeldendeVedtatteVurderinger: List<OvergangUføreVurderingResponse>,
    val historiskeVurderinger: List<OvergangUføreVurderingResponse>,
    val gjeldendeSykdsomsvurderinger: List<SykdomsvurderingResponse>,
)