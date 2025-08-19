package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangarbeid

import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom.SykdomsvurderingResponse

data class OvergangArbeidGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: OvergangArbeidVurderingResponse?,
    val gjeldendeVedtatteVurderinger: List<OvergangArbeidVurderingResponse>,
    val historiskeVurderinger: List<OvergangArbeidVurderingResponse>,
    val gjeldendeSykdsomsvurderinger: List<SykdomsvurderingResponse>,
)