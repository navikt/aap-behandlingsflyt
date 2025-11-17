package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.komponenter.type.Periode

data class InntektsPeriode(
    val periode: Periode,
    val beløp: Double
)

data class InntektData(
    val beløp: Double,
)

