package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal

data class VurdertPeriode(
    val periode: Periode,
    val felter: Felter
)

data class Felter(
    val dagsats: BigDecimal,
    val barneTilleggsats: BigDecimal,
    val arbeidGradering: Int?,
    val samordningGradering: Int?,
    val institusjonGradering: Int?,
    val arbeidsgiverGradering: Int?,
    val totalReduksjon: Int?,
    val effektivDagsats: Double,
)

fun List<VurdertPeriode>.komprimerLikeFelter(): List<VurdertPeriode> {
    return this
        .sortedBy { it.periode.fom }
        .groupBy { it.felter }
        .flatMap { (_, perioderMedLikeFelter) ->
            perioderMedLikeFelter.map { Segment(it.periode, it.felter) }
                .let(::Tidslinje).komprimer()
                .toList()
                .map { VurdertPeriode(it.periode, it.verdi) }
        }
}
