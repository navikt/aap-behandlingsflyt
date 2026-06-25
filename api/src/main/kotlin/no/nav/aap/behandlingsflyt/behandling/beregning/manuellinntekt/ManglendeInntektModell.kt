package no.nav.aap.behandlingsflyt.behandling.beregning.manuellinntekt

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal

data class ManuellInntektGrunnlagResponse(
    val sisteRelevanteÅr: Int,
    val manuelleVurderinger: ManuellInntektGrunnlagVurdering? = null,
    val historiskeManuelleVurderinger: List<ManuellInntektGrunnlagVurdering>? = emptyList(),
    val registrerteInntekterSisteRelevanteAr: List<ÅrData> = emptyList(),
    val harTilgangTilÅSaksbehandle: Boolean,
    val manglendeMånedsInntekter: List<MånedsperiodeData> = emptyList(),
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
    val periode: Periode? = null,
)

data class MånedsperiodeData(
    val år: Int,
    val periode: Periode,
    val uføregrad: Int,
)