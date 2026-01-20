package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

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