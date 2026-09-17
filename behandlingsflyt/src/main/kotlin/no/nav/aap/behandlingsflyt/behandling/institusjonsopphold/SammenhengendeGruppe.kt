package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class SammenhengendeGruppe<T>(val elementer: List<T>, val periode: Periode)

typealias SammenhengendeOppholdGruppe = SammenhengendeGruppe<Segment<Institusjon>>

fun <T> grupperSammenhengende(
    elementer: List<T>,
    fom: (T) -> LocalDate,
    tom: (T) -> LocalDate,
    erSammenhengende: (sistePeriode: Periode, nesteFom: LocalDate) -> Boolean
): List<SammenhengendeGruppe<T>> {
    return elementer
        .sortedBy(fom)
        .fold(mutableListOf<SammenhengendeGruppe<T>>()) { grupper, element ->
            val siste = grupper.lastOrNull()
            if (siste != null && erSammenhengende(siste.periode, fom(element))) {
                grupper[grupper.lastIndex] = SammenhengendeGruppe(
                    siste.elementer + element,
                    Periode(siste.periode.fom, maxOf(siste.periode.tom, tom(element)))
                )
            } else {
                grupper += SammenhengendeGruppe(listOf(element), Periode(fom(element), tom(element)))
            }
            grupper
        }
}

fun <T> finnRelevanteInnenforPeriode(
    elementer: List<T>,
    periode: Periode,
    fom: (T) -> LocalDate,
    tom: (T) -> LocalDate,
    erSammenhengende: (sistePeriode: Periode, nesteFom: LocalDate) -> Boolean
): List<T> {
    return grupperSammenhengende(elementer, fom, tom, erSammenhengende)
        .filter { it.periode.overlapper(periode) }
        .flatMap { it.elementer }
}