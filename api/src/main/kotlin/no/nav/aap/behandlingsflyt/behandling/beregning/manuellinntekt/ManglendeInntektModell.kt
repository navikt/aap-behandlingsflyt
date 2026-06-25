package no.nav.aap.behandlingsflyt.behandling.beregning.manuellinntekt

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import java.math.BigDecimal

data class ManuellInntektGrunnlagResponse(
    @Deprecated("Brukes ikke av frontend snart.") val sisteRelevanteÅr: Int,
    val manglerInntektForÅr: List<Int>,
    val manuelleVurderinger: ManuellInntektGrunnlagVurdering? = null,
    val historiskeManuelleVurderinger: List<ManuellInntektGrunnlagVurdering>? = emptyList(),
    val registrerteInntekterSisteRelevanteAr: List<ÅrData> = emptyList(),
    val harTilgangTilÅSaksbehandle: Boolean,
)

data class ManuellInntektGrunnlagVurdering(
    val begrunnelse: String,
    val vurderingerMeta: VurderingerMetaResponse,
    val årsVurderinger: List<ÅrData>,
)

data class ÅrData(
    val år: Int,
    val beløp: BigDecimal?,
    val eøsBeløp: BigDecimal? = null,
    val ferdigLignetPGI: BigDecimal? = null,
)