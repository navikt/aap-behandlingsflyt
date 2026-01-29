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
    val uføregradTom: LocalDate? = null,
)

fun Collection<Uføre>.tilTidslinje(): Tidslinje<Prosent> {
    return this
        .sortedBy { it.virkningstidspunkt }
        .somTidslinje({ Periode(it.virkningstidspunkt, it.uføregradTom ?: Tid.MAKS) }, { it.uføregrad })
}