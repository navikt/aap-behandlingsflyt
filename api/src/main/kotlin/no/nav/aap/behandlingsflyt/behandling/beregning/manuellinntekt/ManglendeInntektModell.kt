package no.nav.aap.behandlingsflyt.behandling.beregning.manuellinntekt

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import java.math.BigDecimal

@Deprecated("Erstattes av vurderinger")
data class ManuellInntektVurderingGrunnlagResponse(
    val begrunnelse: String,
    val vurdertAv: VurdertAvResponse,
    val ar: Int,
    val belop: BigDecimal,
)

/**
 * @param [ar] Året som det skal gjøres vurdering for. Er med i begge objektene fordi de teoretisk kan være forskjellige.
 */
data class ManuellInntektGrunnlagResponse(
    @Deprecated("Erstattes av vurderinger") val ar: Int,
    @Deprecated("Erstattes av vurderinger") val gverdi: BigDecimal,
    @Deprecated("Erstattes av vurderinger") val vurdering: ManuellInntektVurderingGrunnlagResponse?,
    @Deprecated("Erstattes av vurderinger") val historiskeVurderinger: List<ManuellInntektVurderingGrunnlagResponse>,
    val manuelleVurderinger: ManuellInntektGrunnlagVurdering? = null,
    val historiskeManuelleVurderinger: List<ManuellInntektGrunnlagVurdering>? = emptyList(),
    val registrerteInntekterSisteRelevanteAr: List<AarData> = emptyList(),
    val harTilgangTilÅSaksbehandle: Boolean,
)

data class ManuellInntektGrunnlagVurdering(
    val begrunnelse: String,
    val vurdertAv: VurdertAvResponse,
    val aarsVurderinger: List<AarData>,
)

data class AarData(
    val ar: Int,
    val belop: BigDecimal?,
    val gverdi: BigDecimal,
    val eosBelop: BigDecimal? = null,
)