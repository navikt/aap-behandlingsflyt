package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg

import no.nav.aap.behandlingsflyt.behandling.barnetillegg.RettTilBarnetillegg
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje


data class BarnetilleggGrunnlag(val id: Long, val perioder: List<BarnetilleggPeriode>)

fun List<BarnetilleggPeriode>?.tilTidslinje(): Tidslinje<RettTilBarnetillegg> {
    if (this == null) return Tidslinje.empty()

    return this.filter { !it.personIdenter.isEmpty() }
        .map { Segment(it.periode, RettTilBarnetillegg(it.personIdenter)) }.let(::Tidslinje)
}