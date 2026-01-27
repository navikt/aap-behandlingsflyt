package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway

import java.time.LocalDate

class DagpengerResponse(
    val personIdent: String,
    val perioder: List<DagpengerPeriode>
)

class DagpengerPeriode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val kilde: String,
    val ytelseType: String
)