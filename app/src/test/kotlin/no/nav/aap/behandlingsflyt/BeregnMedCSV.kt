package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.beregning.Beregning
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.BeregnTilkjentYtelseService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.BeregningInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisperiodeId
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import java.io.InputStream
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
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
    val personKode: Int,
    val fødselsdato: Fødselsdato
)

fun readCSV(inputStream: InputStream): List<CSVLine> {
    val reader = inputStream.bufferedReader()

    // Fjerne header
    reader.readLine()

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
                personKode = splitted[12].toInt(),
                fødselsdato = Fødselsdato(LocalDate.parse(splitted[13]))
            )
        }.toList()
}

fun tilInput(csvLine: CSVLine): Pair<BeregningInput, Fødselsdato> {

    return Pair(
        BeregningInput(
            nedsettelsesDato = LocalDate.of(csvLine.beregningsAar, 1, 1),
            inntekter = csvLine.let { (intSiste, intNestSiste, intTredjeSiste, _, inntektSisteAar, inntektNestSisteAar, inntektTredjeSisteAar) ->
                setOf(
                    InntektPerÅr(inntektSisteAar, Beløp(intSiste)),
                    InntektPerÅr(inntektNestSisteAar, Beløp(intNestSiste)),
                    InntektPerÅr(inntektTredjeSisteAar, Beløp(intTredjeSiste))
                )
            },
            uføregrad = emptySet(),
            yrkesskadevurdering = null,
            registrerteYrkesskader = null,
            beregningGrunnlag = null
        ), csvLine.fødselsdato
    )
}

fun beregnForInput(input: BeregningInput, fødselsdato: Fødselsdato): Triple<Year, GUnit, Double> {
    val beregnet = Beregning(Inntektsbehov((input))).beregneMedInput()

    val tilkjent = BeregnTilkjentYtelseService(
        fødselsdato = fødselsdato,
        beregningsgrunnlag = beregnet,
        underveisgrunnlag = UnderveisGrunnlag(
            id = 0,
            perioder = listOf(
                Underveisperiode(
                    periode = Periode(LocalDate.now().withMonth(6), LocalDate.now().plusMonths(12)),
                    meldePeriode = Periode(LocalDate.MIN, LocalDate.now().plusMonths(12)),
                    utfall = Utfall.OPPFYLT,
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    avslagsårsak = null,
                    grenseverdi = Prosent.`100_PROSENT`,
                    arbeidsgradering = ArbeidsGradering(
                        totaltAntallTimer = TimerArbeid(
                            antallTimer = BigDecimal(0)
                        ),
                        andelArbeid = Prosent.`100_PROSENT`,
                        fastsattArbeidsevne = Prosent.`100_PROSENT`, // TODO
                        gradering = Prosent.`100_PROSENT`,
                        opplysningerMottatt = null,
                    ),
                    trekk = Dagsatser(0),
                    brukerAvKvoter = emptySet(),
                    id = UnderveisperiodeId(0),
                    institusjonsoppholdReduksjon = Prosent(0),
                    meldepliktStatus = null,
                )
            )
        ),
        barnetilleggGrunnlag = BarnetilleggGrunnlag(
            perioder = emptyList()
        ),
        samordningGrunnlag = SamordningGrunnlag(emptySet()),
        samordningUføre = null,
        samordningArbeidsgiver = null
    )

    val dagsats = tilkjent.beregnTilkjentYtelse().mapValue { it.dagsats }.komprimer().segmenter().first().verdi.verdi

    return Triple(Year.of(input.nedsettelsesDato.year), beregnet.grunnlaget(), dagsats.toDouble())
}

fun printRad(arenaBeløp: Int, beregnetGUnit: GUnit, personKode: Int, dagsats: Double) {
    val arenaGUnit =
        BigDecimal(arenaBeløp).divide(
            Grunnbeløp.tilTidslinje().segment(Year.of(2025).atDay(200))!!.verdi.verdi, MathContext(5, RoundingMode.HALF_UP)
        )

    val grunnBeløp = grunnBeløpDetteÅret(Year.of(2025))
    val iKroner = beregnetGUnit.multiplisert(grunnBeløp)

    // FRA_ARENA_BELOP,FRA_ARENA_GUNIT,FRA_KELVIN_BELOP,FRA_KELVIN_GUNIT,DIFF_PROSENT,PERSON_KODE,DAGSATS
    println(
        "$arenaBeløp,${arenaGUnit},${iKroner.verdi},${beregnetGUnit.verdi()},${
            prosentDiff(
                arenaGUnit,
                beregnetGUnit.verdi()
            )
        },$personKode,$dagsats"
    )
}

fun prosentDiff(a: BigDecimal, b: BigDecimal): BigDecimal {
    return (b - a) / a
}

fun main() {
    val readRows = readCSV(System.`in`)

    println("FRA_ARENA_BELOP,FRA_ARENA_GUNIT,FRA_KELVIN_BELOP,FRA_KELVIN_GUNIT,DIFF_PROSENT,PERSON_KODE,DAGSATS")
    for (row in readRows) {
        val (inp, år) = tilInput(row)
        val beregnForInput = try {
            beregnForInput(inp, år)
        } catch (e: Exception) {
            print("FEILET FOR INPUT: $row.")
            throw e
        }
        val beregnet = beregnForInput.second
        val dagsats = beregnForInput.third
        printRad(row.grunnlagFraArena, beregnet, row.personKode, dagsats)
    }
}

private fun grunnBeløpDetteÅret(årstall: Year): Beløp {
    return Grunnbeløp.tilTidslinje().segment(årstall.atDay(200))!!.verdi
}
