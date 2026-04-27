package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class Uføre(
    val virkningstidspunkt: LocalDate,
    val uføregrad: Prosent,
    val uføregradFom: LocalDate? = null,
    val uføregradTom: LocalDate? = null,
)

private fun skalSetteSluttdatoPåSegmentPgaReellStans(uføre1: Uføre, uføre2: Uføre): Boolean {
    // TODO Hvis fom for neste segment ikke finnes kan vi ikke vite om det er stans eller ikke
    if (uføre1.uføregradTom == null || uføre2.uføregradFom == null) return false
    return uføre1.uføregradTom.isBefore(uføre2.uføregradFom.minusDays(1))
}

private fun Collection<Uføre>.utledRiktigSluttdatoForSegment(uføre: Uføre): LocalDate {
    val sortertListe = this.sortedBy { it.virkningstidspunkt }
    val indeksForSegment = sortertListe.indexOf(uføre)

    if (sortertListe.size == 1 || indeksForSegment == sortertListe.lastIndex) return uføre.uføregradTom ?: Tid.MAKS

    return if (skalSetteSluttdatoPåSegmentPgaReellStans(uføre, sortertListe[indeksForSegment + 1])) {
        uføre.uføregradTom ?: Tid.MAKS
    } else {
        if (indeksForSegment == sortertListe.lastIndex) {
            Tid.MAKS
        } else {
            val nesteSegment = sortertListe[indeksForSegment + 1]
            nesteSegment.virkningstidspunkt.minusDays(1)
        }
    }
}

fun Collection<Uføre>.tilTidslinje(): Tidslinje<Prosent> {
    return this
        .sortedBy { it.virkningstidspunkt }
        .somTidslinje({
            Periode(
                it.virkningstidspunkt, this.utledRiktigSluttdatoForSegment(it)
            )
        }, { it.uføregrad })
}