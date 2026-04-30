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
    if (uføre1.uføregradTom == null || uføre2.uføregradFom == null) return false
    return uføre1.uføregradTom.isBefore(uføre2.uføregradFom.minusDays(1))
}

private fun Collection<Uføre>.utledRiktigSluttdatoForSegment(uføre: Uføre): LocalDate {
    val sortertListe = this.sortedBy { it.virkningstidspunkt }
    val indeksForSegment = sortertListe.indexOf(uføre)

    return when {
        indeksForSegment == sortertListe.lastIndex -> uføre.uføregradTom ?: Tid.MAKS
        skalSetteSluttdatoPåSegmentPgaReellStans(uføre, sortertListe[indeksForSegment + 1]) -> uføre.uføregradTom
            ?: Tid.MAKS

        else -> sortertListe[indeksForSegment + 1].virkningstidspunkt.minusDays(1)
    }
}


/**
 * Utleder en tidslinje av uføregrad basert på en liste av [Uføre]-perioder.
 * Tidslinjen lages i utgangspunktet fra virkningstidspunkt til virkningstidspunkt
 * Unntaket er når uføregradTom for en gitt periode _ikke_ er dagen før uføregradFom i neste periode. Da har det vært en reell stans i utbetaling av uføre mellom disse datoene.
 * Se slack-tråd for mer detaljer: https://nav-it.slack.com/archives/C06NKNY1399/p1776925834329269
 **/
fun Collection<Uføre>.tilTidslinje(): Tidslinje<Prosent> {
    return this
        .sortedBy { it.virkningstidspunkt }
        .somTidslinje({
            Periode(
                it.virkningstidspunkt, this.utledRiktigSluttdatoForSegment(it)
            )
        }, { it.uføregrad })
}