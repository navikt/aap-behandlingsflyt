package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode

data class TilkjentYtelsePeriode(val periode: Periode, val tilkjent: Tilkjent)

fun List<TilkjentYtelsePeriode>.tilTidslinje() = this.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)