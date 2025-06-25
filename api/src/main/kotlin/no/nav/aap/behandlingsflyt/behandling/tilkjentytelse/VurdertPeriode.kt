package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Objects

data class VurdertPeriode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val felter: Felter
)

data class Felter(
    val dagsats: BigDecimal,
    val barneTilleggsats: BigDecimal,
    val arbeidGradering: Int?,
    val samordningGradering: Int?,
    val institusjonGradering: Int?,
    val totalReduksjon: Int?,
    val effektivDagsats: Double,
) {
    override fun equals(other: Any?): Boolean {
        return other is Felter &&
                dagsats == other.dagsats &&
                barneTilleggsats == other.barneTilleggsats &&
                arbeidGradering == other.arbeidGradering &&
                samordningGradering == other.samordningGradering &&
                institusjonGradering == other.institusjonGradering &&
                totalReduksjon == other.totalReduksjon &&
                effektivDagsats == other.effektivDagsats
    }

    override fun hashCode(): Int {
        return Objects.hash(
            dagsats,
            barneTilleggsats,
            arbeidGradering,
            samordningGradering,
            institusjonGradering,
            totalReduksjon,
            effektivDagsats
        )
    }
}

fun List<VurdertPeriode>.komprimerLikeFelter(): List<VurdertPeriode> {
    return this
        .sortedBy { it.fraOgMed }
        .groupBy { it.felter }
        .flatMap { (_, perioderMedLikeFelter) ->
            perioderMedLikeFelter
                .sortedBy { it.fraOgMed }
                .fold(mutableListOf()) { acc, periode ->
                    val siste = acc.lastOrNull()
                    if (siste != null && (siste.tilOgMed.plusDays(1) >= periode.fraOgMed)) {
                        acc[acc.lastIndex] = siste.copy(tilOgMed = maxOf(siste.tilOgMed, periode.tilOgMed))
                    } else {
                        acc.add(periode)
                    }
                    acc
                }
        }
}
