package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import java.math.BigDecimal

data class ManuellInntektVurderingDto(
    val begrunnelse: String,
    val belop: BigDecimal,
)