package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp

data class InntektsPeriode(
    val periode: Periode,
    val beløp: Beløp
)

data class InntektData(
    val beløp: Double,
)

