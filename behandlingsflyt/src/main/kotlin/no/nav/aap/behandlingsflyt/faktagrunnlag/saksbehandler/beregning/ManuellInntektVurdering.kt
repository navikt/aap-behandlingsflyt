package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import java.math.BigDecimal
import java.time.Year

data class ManuellInntektVurdering(
    val år: Year,
    val begrunnelse: String,
    val belop: BigDecimal,
    val vurdertAv: String
)