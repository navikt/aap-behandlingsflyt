package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import java.math.BigDecimal
import java.time.Year

data class ManuellInntektVurderingDto(
    val begrunnelse: String,
    @Deprecated("Ikke bruk, skal over til flere vurderinger") val belop: BigDecimal,
    val vurderinger: List<ManuellInntekterVurderingDto>?
)

data class ManuellInntekterVurderingDto(
    val belop: BigDecimal,
    val år: Year
)