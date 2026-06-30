package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal

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
    val periode: Periode? = null,
)