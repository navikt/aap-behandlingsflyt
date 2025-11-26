package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import java.math.BigDecimal

data class ManuellInntektVurderingDto(
    val begrunnelse: String,
    @Deprecated("Ikke bruk, skal over til flere vurderinger") val belop: BigDecimal,
    val vurderinger: List<AarsVurdering>? = emptyList(),
)

data class AarsVurdering(
    val belop: BigDecimal?,
    val eosBelop: BigDecimal?,
    val ar: Int
)