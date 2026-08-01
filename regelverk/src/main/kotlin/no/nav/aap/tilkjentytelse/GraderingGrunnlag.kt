package no.nav.aap.tilkjentytelse

import no.nav.aap.komponenter.verdityper.Prosent

data class GraderingGrunnlag(
    val samordningGradering: Prosent,
    val institusjonGradering: Prosent,
    val arbeidGradering: Prosent,
    val samordningUføregradering: Prosent,
    val samordningArbeidsgiverGradering: Prosent,
    val meldepliktGradering: Prosent,
) {
    @Suppress("FunctionName")
    fun graderingForDagsats11_9Reduksjon() = Prosent.`100_PROSENT`
        .minus(samordningGradering)
        .minus(samordningArbeidsgiverGradering)
        .minus(institusjonGradering)
        .minus(samordningUføregradering)
        .minus(meldepliktGradering)
}