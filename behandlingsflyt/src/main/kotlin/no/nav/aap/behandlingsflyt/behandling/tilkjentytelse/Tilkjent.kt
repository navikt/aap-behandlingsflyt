package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`100_PROSENT`
import java.math.RoundingMode
import java.time.LocalDate

/**
 * @param grunnlagsfaktor Dagsats = grunnbeløp * grunnlagsfaktor
 * @param grunnbeløp Grunnbeløp i denne perioden.
 * @param dagsats Daglig utbetaling i denne perioden.
 */
data class Tilkjent(
    val dagsats: Beløp,
    val gradering: Prosent,
    val graderingGrunnlag: GraderingGrunnlag,
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
            dagsats.multiplisert(gradering)
                .pluss(barnetillegg.multiplisert(gradering)).verdi().setScale(0, RoundingMode.HALF_UP)
        )
    }

    fun dagsatsFor11_9Reduksjon(): Beløp {
        return Beløp(
            dagsats.multiplisert(graderingGrunnlag.graderingForDagsats11_9Reduksjon())
                .pluss(barnetillegg.multiplisert(graderingGrunnlag.graderingForDagsats11_9Reduksjon())).verdi().setScale(0, RoundingMode.HALF_UP)
        )
    }
}

data class GraderingGrunnlag(
    val samordningGradering: Prosent,
    val institusjonGradering: Prosent,
    val arbeidGradering: Prosent,
    val samordningUføregradering: Prosent,
    val samordningArbeidsgiverGradering: Prosent,
    val meldepliktGradering: Prosent,
) {
    fun graderingForDagsats11_9Reduksjon() = `100_PROSENT`
        .minus(samordningGradering)
        .minus(samordningArbeidsgiverGradering)
        .minus(institusjonGradering)
        .minus(samordningUføregradering)
        .minus(meldepliktGradering)
}