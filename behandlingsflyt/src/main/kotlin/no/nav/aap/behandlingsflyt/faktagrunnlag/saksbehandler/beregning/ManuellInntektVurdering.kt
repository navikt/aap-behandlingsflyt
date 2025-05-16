package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.komponenter.verdityper.Beløp
import java.math.BigDecimal
import java.time.Year

data class ManuellInntektVurdering(
    val år: Year,
    val begrunnelse: String,
    val belop: Beløp,
    val vurdertAv: String
)