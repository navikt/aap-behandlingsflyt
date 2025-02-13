package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.beregning.Beregning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import java.io.InputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year

/**
 * Fungerer kun for tilfellet uten uføre/yrkesskade per nå.
 *
 * Bruk:
 *
 * cat AAP_SyntetiskData.csv | ./gradlew -q beregnCSV > output.csv
 *
 * Her antas skjema å være på formen:
 *
 *  - INTSISTE - Inntekt siste år
 *  - INTNESTS - Inntekt nest siste år
 *  - INTTRDS - Inntekt tredje siste år
 *  - AAPBER - Årstall, beregningsdato
 *  - INTARSISTE - årstall, siste inntektsår
 *  - INTARNESTS - årstall, nest siste inntektsår
 *  - INTARTREDS - årstall, tredje siste beregningsår
 *  - AYRKESSKADE - har yrkesskade, 1 = JA
 *  - YSKADEGRD - Prosent yrkesskadegrad
 *  -SAM - Samordning utført i arena, 1 = JA. Tolkes som "har uføre"
 *  - GRUNN - Kroner,  Grunnlag regnet ut i Arena.
 *  - PersonKode - koblingsnøkkel til vedtak id
 */

data class CSVLine(
    val intSiste: Int,
    val intNestSiste: Int,
    val intTredjeSiste: Int,
    val beregningsAar: Int,
    val inntektSisteAar: Int,
    val inntektNestSisteAar: Int,
    val inntektTredjeSisteAar: Int,
    val grunnlagFraArena: Int,
    val personKode: Int
)

fun readCSV(inputStream: InputStream): List<CSVLine> {
    val reader = inputStream.bufferedReader()
    val header = reader.readLine()

    return reader.lineSequence()
        .filter { it.isNotBlank() }
        .map {
            val splitted = it.split(',')
            CSVLine(
                intSiste = splitted[1].toInt(),
                intNestSiste = splitted[2].toInt(),
                intTredjeSiste = splitted[3].toInt(),
                beregningsAar = splitted[4].toInt(),
                inntektSisteAar = splitted[5].toInt(),
                inntektNestSisteAar = splitted[6].toInt(),
                inntektTredjeSisteAar = splitted[7].toInt(),
                grunnlagFraArena = splitted[11].toInt(),
                personKode = splitted[12].toInt()
            )
        }.toList()
}

fun tilInput(csvLine: CSVLine): Input{

    return Input(
        nedsettelsesDato = LocalDate.of(csvLine.beregningsAar, 1, 1),
        inntekter = csvLine.let { (intSiste, intNestSiste, intTredjeSiste, _, inntektSisteAar, inntektNestSisteAar, inntektTredjeSisteAar) ->
            setOf(
                InntektPerÅr(inntektSisteAar, Beløp(intSiste)),
                InntektPerÅr(inntektNestSisteAar, Beløp(intNestSiste)),
                InntektPerÅr(inntektTredjeSisteAar, Beløp(intTredjeSiste))
            )
        },
        uføregrad = null,
        yrkesskadevurdering = null,
        registrerteYrkesskader = null,
        beregningGrunnlag = null
    )
}

fun beregnForInput(input: Input): Pair<Year, GUnit> {
    val beregnet = Beregning(Inntektsbehov((input))).beregneMedInput().grunnlaget()

    return Pair(Year.of(input.nedsettelsesDato.year), beregnet)
}

fun printRad(år: Year, arenaBeløp: Int, beregnetGUnit: GUnit, personKode: Int) {
    val arenaGUnit =
        Grunnbeløp.finnGUnit(år.atDay(355), Beløp(arenaBeløp)).gUnit

    val grunnBeløp = grunnBeløpDetteÅret(år)
    val iKroner = beregnetGUnit.multiplisert(grunnBeløp)

    println(
        "$arenaBeløp,${arenaGUnit.verdi()},${iKroner.verdi},${beregnetGUnit.verdi()},${
            prosentDiff(
                arenaGUnit.verdi(),
                beregnetGUnit.verdi()
            )
        },$personKode"
    )
}

fun prosentDiff(a: BigDecimal, b: BigDecimal): BigDecimal {
    return (b - a) / a
}

fun main() {
    val readRows = readCSV(System.`in`)

    println("FRA_ARENA_BELOP,FRA_ARENA_GUNIT,FRA_KELVIN_BELOP,FRA_KELVIN_GUNIT,DIFF_PROSENT,PERSON_KODE")
    for (row in readRows) {
        val beregnet = beregnForInput(tilInput(row)).second
        printRad(Year.of(row.beregningsAar), row.grunnlagFraArena, beregnet, row.personKode)
    }
}

private fun grunnBeløpDetteÅret(årstall: Year): Beløp {
    return Grunnbeløp.tilTidslinjeGjennomsnitt().segment(årstall.atDay(1))!!.verdi
}
