package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid

/**
 * Finner en fiktiv én-dagsperiode, fra og med [Tid.MIN], som ikke overlapper rettighetsperioden
 * til noen av de andre sakene til personen. Brukes for trukne saker, slik at de ikke overlapper
 * med nye saker, og slik at periodene deres er unike.
 */
fun SakRepository.finnUgyldigRettighetsperiode(sakId: SakId): Periode {
    val førsteLedigeDato = finnSakerFor(finnPersonId(sakId))
        .filter { it.id != sakId }
        .somTidslinje({ it.rettighetsperiode }, { false }).komprimer()
        .mergePrioriterVenstre(tidslinjeOf(Periode(Tid.MIN, Tid.MAKS) to true))
        .filter { it.verdi }
        .perioder()
        .first()
        .fom
    return Periode(førsteLedigeDato, førsteLedigeDato)
}
