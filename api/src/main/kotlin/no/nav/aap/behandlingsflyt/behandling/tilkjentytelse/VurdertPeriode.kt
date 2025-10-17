package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode

data class VurdertPeriode(
    val periode: Periode,
    val felter: Felter
)

data class Felter(
    val dagsats: Double,
    val barneTilleggsats: Double,
    val arbeidGradering: Int?,
    val samordningGradering: Int?,
    val institusjonGradering: Int?,
    val arbeidsgiverGradering: Int?,
    val totalReduksjon: Int?,
    val effektivDagsats: Double,
)

fun List<VurdertPeriode>.komprimerLikeFelter(): List<VurdertPeriode> {
    return this
        .groupBy { it.felter }
        .flatMap { (_, perioderMedLikeFelter) ->
            perioderMedLikeFelter.map { Segment(it.periode, it.felter) }
                .let(::Tidslinje).komprimer()
                .segmenter()
                .map { VurdertPeriode(it.periode, it.verdi) }
        }
        .sortedBy { it.periode.fom }
}
