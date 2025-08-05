package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.beregning.Beregning
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.BeregnTilkjentYtelseService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisperiodeId
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktId
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
                personKode = splitted[12].toInt(),
                fødselsdato = Fødselsdato(LocalDate.parse(splitted[13]))
            )
        }.toList()
}

fun tilInput(csvLine: CSVLine): Pair<Input, Fødselsdato> {

    return Pair(
        Input(
            nedsettelsesDato = LocalDate.of(csvLine.beregningsAar, 1, 1),
            inntekter = csvLine.let { (intSiste, intNestSiste, intTredjeSiste, _, inntektSisteAar, inntektNestSisteAar, inntektTredjeSisteAar) ->
                setOf(
                    InntektPerÅr(inntektSisteAar, Beløp(intSiste)),
                    InntektPerÅr(inntektNestSisteAar, Beløp(intNestSiste)),
                    InntektPerÅr(inntektTredjeSisteAar, Beløp(intTredjeSiste))
                )
            },
            uføregrad = emptyList(),
            yrkesskadevurdering = null,
            registrerteYrkesskader = null,
            beregningGrunnlag = null
        ), csvLine.fødselsdato
    )
}

fun beregnForInput(input: Input, fødselsdato: Fødselsdato): Triple<Year, GUnit, Double> {
    val beregnet = Beregning(Inntektsbehov((input))).beregneMedInput()

    val tilkjent = BeregnTilkjentYtelseService(
        fødselsdato = fødselsdato,
        beregningsgrunnlag = beregnet,
        underveisgrunnlag = UnderveisGrunnlag(
            id = 0,
            perioder = listOf(
                Underveisperiode(
                    periode = Periode(LocalDate.now().withMonth(1), LocalDate.MAX),
                    meldePeriode = Periode(LocalDate.MIN, LocalDate.MAX),
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
                    brukerAvKvoter = setOf(),
                    bruddAktivitetspliktId = BruddAktivitetspliktId(0),
                    id = UnderveisperiodeId(0),
                    institusjonsoppholdReduksjon = TODO(),
                    meldepliktStatus = null,
                )
            )
        ),
        barnetilleggGrunnlag = BarnetilleggGrunnlag(
            id = 0,
            perioder = listOf()
        ),
        samordningGrunnlag = SamordningGrunnlag(0L, listOf()),
        samordningUføre = null,
        samordningArbeidsgiver = null
    )

    val dagsats = tilkjent.beregnTilkjentYtelse().mapValue { it.dagsats }.komprimer().first().verdi.verdi

    return Triple(Year.of(input.nedsettelsesDato.year), beregnet.grunnlaget(), dagsats.toDouble())
}

fun printRad(år: Year, arenaBeløp: Int, beregnetGUnit: GUnit, personKode: Int, dagsats: Double) {
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
        val beregnForInput = beregnForInput(inp, år)
        val beregnet = beregnForInput.second
        val dagsats = beregnForInput.third
        printRad(Year.of(row.beregningsAar), row.grunnlagFraArena, beregnet, row.personKode, dagsats)
    }
}

private fun grunnBeløpDetteÅret(årstall: Year): Beløp {
    return Grunnbeløp.tilTidslinjeGjennomsnitt().segment(årstall.atDay(1))!!.verdi
}
