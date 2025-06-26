package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import java.math.RoundingMode
import java.time.LocalDate

/**
 * @param grunnlag Beregningsgrunnlag som er lagt til grunn før beregning av dagsats og før gradering er beregnet.
 */
class Tilkjent(
    val dagsats: Beløp,
    val gradering: TilkjentGradering,
    val grunnlag: Beløp,
    val grunnlagsfaktor: GUnit,
    val grunnbeløp: Beløp,
    val antallBarn: Int,
    val barnetilleggsats: Beløp,
    val barnetillegg: Beløp,
    val utbetalingsdato: LocalDate
) {

    /**
     * Hent ut full dagsats etter reduksjon.
     */
    fun redusertDagsats(): Beløp {
        return Beløp(
            dagsats.multiplisert(gradering.endeligGradering)
                .pluss(barnetillegg.multiplisert(gradering.endeligGradering)).verdi().setScale(0, RoundingMode.HALF_UP)
        )
    }

    override fun toString(): String {
        return "Tilkjent(dagsats=$dagsats, gradering=$gradering, redusertDagsats=${redusertDagsats()}), grunnlag=$grunnlag, grunnlagsfaktor=$grunnlagsfaktor, grunnbeløp=$grunnbeløp, antallBarn=$antallBarn, barnetilleggsats=$barnetilleggsats, barnetillegg=$barnetillegg, utbetalingsdato=$utbetalingsdato)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tilkjent

        if (antallBarn != other.antallBarn) return false
        if (dagsats != other.dagsats) return false
        if (gradering != other.gradering) return false
        if (grunnlag != other.grunnlag) return false
        if (grunnlagsfaktor != other.grunnlagsfaktor) return false
        if (grunnbeløp != other.grunnbeløp) return false
        if (barnetilleggsats != other.barnetilleggsats) return false
        if (barnetillegg != other.barnetillegg) return false
        if (utbetalingsdato != other.utbetalingsdato) return false

        return true
    }

    override fun hashCode(): Int {
        var result = antallBarn
        result = 31 * result + dagsats.hashCode()
        result = 31 * result + gradering.hashCode()
        result = 31 * result + grunnlag.hashCode()
        result = 31 * result + grunnlagsfaktor.hashCode()
        result = 31 * result + grunnbeløp.hashCode()
        result = 31 * result + barnetilleggsats.hashCode()
        result = 31 * result + barnetillegg.hashCode()
        result = 31 * result + utbetalingsdato.hashCode()
        return result
    }
}

data class TilkjentGradering(
    val endeligGradering: Prosent,
    val samordningGradering: Prosent?,
    val institusjonGradering: Prosent?,
    val arbeidGradering: Prosent?,
    val samordningUføregradering: Prosent?
)

data class TilkjentGUnit(val dagsats: GUnit, val gradering: TilkjentGradering, val utbetalingsdato: LocalDate) {
    private fun redusertDagsats(): GUnit {
        return dagsats.multiplisert(gradering.endeligGradering)
    }

    override fun toString(): String {
        return "Tilkjent(dagsats=$dagsats, gradering=$gradering, redusertDagsats=${redusertDagsats()})"
    }
}