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
                this.dagsats == other.dagsats &&
                this.barneTilleggsats == other.barneTilleggsats &&
                this.arbeidGradering == other.arbeidGradering &&
                this.samordningGradering == other.samordningGradering &&
                this.institusjonGradering == other.institusjonGradering &&
                this.totalReduksjon == other.totalReduksjon &&
                this.effektivDagsats == other.effektivDagsats
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