package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import java.math.BigDecimal
import java.time.LocalDate

data class ManuellInntektVurderingDto(
    val begrunnelse: String,
    @Deprecated("Ikke bruk, skal over til flere vurderinger") val belop: BigDecimal?,
    val vurderinger: List<ÅrsVurdering>? = emptyList(),
)

data class ÅrsVurdering(
    val beløp: BigDecimal?,
    val eøsBeløp: BigDecimal?,
    val år: Int,
    val ferdigLignetPGI: BigDecimal?,
    /**
     * Delperiode innen [år] når inntekten gjelder en del av året (før/etter endring i uføregrad).
     */
    val periodeFom: LocalDate? = null,
    val periodeTom: LocalDate? = null,
)