package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`100_PROSENT`
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

    /** Antall barn som gir rett til barnetillegg. */
    val antallBarn: Int,

    /** Størrelsen på ugradert barnetilleggsats.
     *
     * Verdien er ugradert, i den forstand at:
     * Hvis barnetilleggsatsen er spesifisert i AAP-forskriften § 8 til 38 kroner, og medlemmet får 50% AAP,
     * så vil [barnetilleggsats] være 38.
     **/
    val barnetilleggsats: Beløp,

    /** Størrelsen på total, ugradert barnetillegg.
     *
     * Verdien er total i den forstand at den tar hensyn til antall barn.
     *
     * Den er ugradert i den forstand at hvis medlemmet har 2 barn, får 50 % AAP
     * på grunn av samordning, og barnetilleggssatsen er spesifisert i AAP-forskriften § 8 til 38 kroner,
     * så vil [barnetillegg] være 2 * 38 = 76. Altså vi har ikke redusert barnetillegget med 50% her.
     *
     * Spesifikasjon: [barnetillegg] = [barnetilleggsats] * [antallBarn].
     */
    val barnetillegg: Beløp,

    val barnepensjonDagsats: Beløp,
    val utbetalingsdato: LocalDate,
    val minsteSats: Minstesats,
    private val redusertDagsats: Beløp?
) {
    init {
        require(barnepensjonDagsats.verdi.compareTo(barnepensjonDagsats.heltallverdi()) == 0) {
            "Barnepensjon dagsats må være et heltall"
        }
    }

    /**
     * Hent ut full dagsats etter reduksjon.
     */
    fun redusertDagsats(): Beløp {
        if (redusertDagsats != null) return redusertDagsats

        /* I historiske behandlinger, så er ikke `redusertDagsats` lagret
         * ned. Regner derfor ut her. Burde vurdere å backfille, slik at
         * vi ikke trenger denne kodesnutten.
         */
        return BeregnTilkjentYtelseService.redusertDagsats(
            dagsats = dagsats,
            barnetillegg = barnetillegg,
            barnepensjonDagsats = barnepensjonDagsats,
            gradering = gradering,
        )
    }

    @Suppress("FunctionName")
    fun dagsatsFor11_9Reduksjon(): Beløp {
        return BeregnTilkjentYtelseService.redusertDagsats(
            dagsats = dagsats,
            barnetillegg = barnetillegg,
            barnepensjonDagsats = barnepensjonDagsats,
            gradering = graderingGrunnlag.graderingForDagsats11_9Reduksjon(),
        )
    }
}

enum class Minstesats { IKKE_MINSTESATS, MINSTESATS_OVER_25, MINSTESATS_UNDER_25 }

data class GraderingGrunnlag(
    val samordningGradering: Prosent,
    val institusjonGradering: Prosent,
    val arbeidGradering: Prosent,
    val samordningUføregradering: Prosent,
    val samordningArbeidsgiverGradering: Prosent,
    val meldepliktGradering: Prosent,
) {
    @Suppress("FunctionName")
    fun graderingForDagsats11_9Reduksjon() = `100_PROSENT`
        .minus(samordningGradering)
        .minus(samordningArbeidsgiverGradering)
        .minus(institusjonGradering)
        .minus(samordningUføregradering)
        .minus(meldepliktGradering)
}

fun maks(a: Beløp, b: Beløp): Beløp {
    return if (a.verdi > b.verdi) a else b
}