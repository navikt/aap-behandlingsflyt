package no.nav.aap.tidslinje

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.JoinStyle.DISJOINT
import no.nav.aap.tidslinje.JoinStyle.INNER_JOIN
import no.nav.aap.tidslinje.JoinStyle.LEFT_JOIN
import no.nav.aap.tidslinje.JoinStyle.OUTER_JOIN
import no.nav.aap.tidslinje.JoinStyle.RIGHT_JOIN
import java.time.LocalDate

infix fun <T> T.fom(fom: String) = this to LocalDate.parse(fom)
infix fun <T> Pair<T, LocalDate>.tom(tom: String) = Segment(Periode(second, LocalDate.parse(tom)), first)
fun <T> tidslinje(vararg segmenter: Segment<T>) = Tidslinje(segmenter.toList())

class TegnTidslinje {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val venstre = tidslinje(
                "x" fom "2020-01-01" tom "2020-01-02",
                "y" fom "2020-01-05" tom "2020-01-05",
            )
            val høyre = tidslinje(
                "1" fom "2020-01-02" tom "2020-01-03",
            )
            printAscii2(venstre, høyre, ::OUTER_JOIN)
            printAscii1(venstre, høyre, ::DISJOINT)
            printAscii2(venstre, høyre, ::LEFT_JOIN)
            printAscii2(venstre, høyre, ::RIGHT_JOIN)
            printAscii2(venstre, høyre, ::INNER_JOIN)
            printBinaryFunction(venstre, høyre, "priorterHøyreSide", StandardSammenslåere.prioriterHøyreSide())
            printBinaryFunction(venstre, høyre, "priorterHøyreSideCrossJoin", StandardSammenslåere.prioriterHøyreSideCrossJoin())
            printBinaryFunction(venstre, høyre, "priorterVenstreSideCrossJoin", StandardSammenslåere.prioriterVenstreSideCrossJoin())
            printBinaryFunction(venstre, høyre, "kunVenstre", StandardSammenslåere.kunVenstre())
            printBinaryFunction(venstre, høyre, "kunHøyreLeftJoin", StandardSammenslåere.kunHøyreLeftJoin())
            printBinaryFunction(venstre, høyre, "kunHøyreRightJoin", StandardSammenslåere.kunHøyreRightJoin())
            printBinaryFunction(venstre, høyre, "minus", StandardSammenslåere.minus())
        }
    }
}

fun <T> printAscii1(
    venstre: Tidslinje<String>,
    høyre: Tidslinje<String>,
    cons: (_: (Periode, Segment<T>?) -> Segment<Any?>) -> JoinStyle<String, String, Any?>,
) {
    printBinaryFunction(venstre, høyre, cons { p, x -> Segment(p, Unary(x?.verdi)) })
}

fun <T, S>printAscii2(
    venstre: Tidslinje<T>,
    høyre: Tidslinje<S>,
    cons: (_: (Periode, Segment<T>?, Segment<S>?) -> Segment<Any?>) -> JoinStyle<T, S, Any?>,
) {
    printBinaryFunction(venstre, høyre, cons { p, l, r -> Segment(p, Binary(l?.verdi, r?.verdi)) })
}

fun <A : Any?, B : Any?, C : Any?> printBinaryFunction(
    venstre: Tidslinje<A>,
    høyre: Tidslinje<B>,
    joinStyle: JoinStyle<A, B, C>,
) {
    val resultat = venstre.kombiner(høyre, joinStyle)
    printBinaryFunction(venstre, høyre, joinStyle::class.simpleName ?: "resultat", resultat)
}

fun <A : Any?, B : Any?, C : Any?> printBinaryFunction(
    venstre: Tidslinje<A>,
    høyre: Tidslinje<B>,
    navn: String,
    joinStyle: JoinStyle<A, B, C>,
) {
    val resultat = venstre.kombiner(høyre, joinStyle)
    printBinaryFunction(venstre, høyre, navn, resultat)
}

class Unary(val x: Any?) {
    override fun toString() = "f($x)"
}
class Binary(val l: Any?, val r: Any?) {
    override fun toString() = "f($l, $r)"
}


fun <A : Any?, B : Any?, C : Any?> printBinaryFunction(
    venstreTidslinje: Tidslinje<A>,
    høyreTidslinje: Tidslinje<B>,
    joinerNavn: String,
    resultatInputTidslinje: Tidslinje<C>
) {
    val resultatTidslinje: Tidslinje<String> =
        venstreTidslinje.kombiner(høyreTidslinje, OUTER_JOIN { p, v, h ->
            Segment(p, Pair(v, h))
        }).kombiner(resultatInputTidslinje, RIGHT_JOIN { p, _, r ->
            Segment(p, r.verdi.toString())
        })

    val perioden =
        venstreTidslinje.helePerioden().utvid(høyreTidslinje.helePerioden()).utvid(resultatTidslinje.helePerioden())
            .let { it.utvid(Periode(it.fom, it.tom.plusDays(1))) }

    println(" ".repeat(12) + "venstre".padEnd(8) + "høyre".padEnd(7) + joinerNavn)

    var printVenstreLinje = false
    var printHøyreLinje = false
    var printResultatLinje = false
    for (dag in Periodeiterator2(perioden)) {
        val venstre = eval(dag, venstreTidslinje.segment(dag))
        val høyre = eval(dag, høyreTidslinje.segment(dag))
        val resultat = eval(dag, resultatTidslinje.segment(dag))

        printVenstreLinje = printVenstreLinje || venstre?.start ?: false
        printHøyreLinje = printHøyreLinje || høyre?.start ?: false
        printResultatLinje = printResultatLinje || resultat?.start ?: false

        print("$dag".padEnd(12))

        printLinje(5, venstre, printVenstreLinje)
        print("   ")
        printLinje(5, høyre, printHøyreLinje)
        print("  ")
        printLinje(14, resultat, printResultatLinje)
        println()

        print(" ".repeat(12))
        printVerdi(5, venstre)
        print("   ")
        printVerdi(5, høyre)
        print("  ")
        printVerdi(14, resultat)
        println()

        printVenstreLinje = venstre?.slutt ?: false
        printHøyreLinje = høyre?.slutt ?: false
        printResultatLinje = resultat?.slutt ?: false
    }
}

class Info(
    val start: Boolean,
    val slutt: Boolean,
    val verdi: String,
)

fun <T : Any?> eval(date: LocalDate, segment: Segment<T>?): Info? {
    if (segment == null) return null
    return Info(
        start = date == segment.fom(),
        verdi = segment.verdi.toString(),
        slutt = date == segment.tom(),
    )
}

fun printLinje(colwidth: Int, info: Info?, printLinje: Boolean) {
    val ends = if (info == null) " " else "|"
    if (printLinje) {
        print("+${"-".repeat(colwidth - 2)}+")
    } else {
        print(ends + " ".repeat(colwidth - 2) + ends)
    }
}

fun printVerdi(colwidth: Int, info: Info?) {
    if (info == null) {
        print(" ".repeat(colwidth))
    } else if (info.start) {
        print("| ")
        print(info.verdi.padEnd(colwidth - 4))
        print(" |")
    } else {
        print("|")
        print(" ".repeat(colwidth - 2))
        print("|")
    }
}

class Periodeiterator2(private val periode: Periode) : Iterator<LocalDate> {
    private var current = periode.fom
    override fun hasNext() = current <= periode.tom
    override fun next() = current.also {
        current = current.plusDays(1)
    }
}
