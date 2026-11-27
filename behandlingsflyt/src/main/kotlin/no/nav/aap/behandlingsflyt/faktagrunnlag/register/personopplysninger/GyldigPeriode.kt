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
/** Faktisk periodeoverlapp. Manglende fra-dato regnes som åpen start og manglende til-dato som åpen slutt. */
fun GyldigPeriode.overlapperPeriode(periode: Periode): Boolean =
    overlapperPeriode(gyldigFraOgMed, gyldigTilOgMed, periode)

fun overlapperPeriode(fra: LocalDate?, til: LocalDate?, periode: Periode): Boolean =
    (fra == null || !fra.isAfter(periode.tom)) && (til == null || !til.isBefore(periode.fom))
