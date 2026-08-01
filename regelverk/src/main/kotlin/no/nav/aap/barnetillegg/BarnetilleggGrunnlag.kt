package no.nav.aap.barnetillegg

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class BarnetilleggGrunnlag(val perioder: List<BarnetilleggPeriode>)

fun List<BarnetilleggPeriode>?.tilTidslinje(): Tidslinje<RettTilBarnetillegg> {
    if (this == null) return Tidslinje.Companion.empty()

    return this
        .filterNot { it.personIdenter.isEmpty() }
        .map { Segment(it.periode, RettTilBarnetillegg(it.personIdenter)) }
        .let(::Tidslinje)
}