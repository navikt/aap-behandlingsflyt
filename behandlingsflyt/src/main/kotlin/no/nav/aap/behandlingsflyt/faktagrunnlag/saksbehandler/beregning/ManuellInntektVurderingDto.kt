package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import java.math.BigDecimal

data class ManuellInntektVurderingDto(
    val begrunnelse: String,
    @Deprecated("Ikke bruk, skal over til flere vurderinger") val belop: BigDecimal,
    val vurderinger: List<ÅrsVurdering>? = emptyList(),
)

data class ÅrsVurdering(
    val beløp: BigDecimal?,
    val eøsBeløp: BigDecimal?,
    val år: Int
)