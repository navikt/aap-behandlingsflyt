package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import java.math.BigDecimal

data class ManuellInntektVurderingDto(
    val år: Int,
    val begrunnelse: String,
    val belop: BigDecimal,
    val vurdertAv: String
)