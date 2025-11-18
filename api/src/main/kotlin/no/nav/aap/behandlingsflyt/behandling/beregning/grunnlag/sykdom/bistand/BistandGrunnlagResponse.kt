package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.bistand

import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom.SykdomsvurderingResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse

data class BistandGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    @Deprecated("Erstattes av vurderinger")
    val vurdering: BistandVurderingResponse?,
    val vurderinger: List<BistandVurderingResponse>,
    val gjeldendeVedtatteVurderinger: List<BistandVurderingResponse>,
    val historiskeVurderinger: List<BistandVurderingResponse>,
    val gjeldendeSykdsomsvurderinger: List<SykdomsvurderingResponse>,
    val harOppfylt11_5: Boolean?, // Slettes når 11-17 er prodsatt
    val kvalitetssikretAv: VurdertAvResponse?
)