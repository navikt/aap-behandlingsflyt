package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import java.math.RoundingMode
import java.time.LocalDate

/**
 * @param grunnlag Beregningsgrunnlag som er lagt til grunn før beregning av dagsats og før gradering er beregnet.
 */
data class Tilkjent(
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
}

data class TilkjentGradering(
    val endeligGradering: Prosent,
    val samordningGradering: Prosent?,
    val institusjonGradering: Prosent?,
    val arbeidGradering: Prosent?,
    val samordningUføregradering: Prosent?,
    val samordningArbeidsgiverGradering: Prosent?
)

data class TilkjentGUnit(val dagsats: GUnit, val gradering: TilkjentGradering, val utbetalingsdato: LocalDate) {
    private fun redusertDagsats(): GUnit {
        return dagsats.multiplisert(gradering.endeligGradering)
    }

    override fun toString(): String {
        return "Tilkjent(dagsats=$dagsats, gradering=$gradering, redusertDagsats=${redusertDagsats()})"
    }
}