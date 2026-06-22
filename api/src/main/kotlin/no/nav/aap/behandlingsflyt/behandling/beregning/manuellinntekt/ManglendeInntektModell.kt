package no.nav.aap.behandlingsflyt.behandling.beregning.manuellinntekt

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import java.math.BigDecimal
import java.time.LocalDate

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
    val sisteRelevanteÅr: Int,
    val manuelleVurderinger: ManuellInntektGrunnlagVurdering? = null,
    val historiskeManuelleVurderinger: List<ManuellInntektGrunnlagVurdering>? = emptyList(),
    val registrerteInntekterSisteRelevanteAr: List<ÅrData> = emptyList(),
    val harTilgangTilÅSaksbehandle: Boolean,
    /**
     * Delperioder for år der uføregraden endrer seg midt i året (én rad per uføregrad-segment),
     * som saksbehandler skal legge inn beregnet PGI for. Tom når funksjonen ikke er aktiv.
     */
    val delperioderForSplittÅr: List<DelperiodeData>? = emptyList(),
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
    val periodeFom: LocalDate? = null,
    val periodeTom: LocalDate? = null,
)

data class DelperiodeData(
    val år: Int,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val uføregrad: Int,
)