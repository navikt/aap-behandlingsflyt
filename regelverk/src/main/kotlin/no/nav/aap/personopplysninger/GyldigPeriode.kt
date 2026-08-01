package no.nav.aap.personopplysninger

import java.time.LocalDate
import no.nav.aap.komponenter.type.Periode

interface GyldigPeriode {
    val gyldigFraOgMed: LocalDate?
    val gyldigTilOgMed: LocalDate?
}

fun GyldigPeriode.erGyldigIPeriode(periode: Periode): Boolean {
    val fra = gyldigFraOgMed
    val til = gyldigTilOgMed

    return (fra != null && periode.inneholder(fra)) || til == null ||
            periode.inneholder(til)
}