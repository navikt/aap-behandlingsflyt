package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.komponenter.type.Periode
import java.time.DayOfWeek
import java.time.LocalDate

@JvmInline
value class Hverdager(private val hverdager: Int) : Comparable<Hverdager> {
    operator fun plus(other: Hverdager) = Hverdager(this.hverdager + other.hverdager)
    operator fun minus(other: Hverdager) = Hverdager(this.hverdager - other.hverdager)
    override fun compareTo(other: Hverdager) = hverdager.compareTo(other.hverdager)

    companion object {
        fun LocalDate.plusHverdager(hverdager: Hverdager): LocalDate {
            return hverdagerFraOgMed(this).elementAt(hverdager.hverdager)
        }

        private val hverdagene = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )

        private val LocalDate.erHverdag: Boolean
            get() = dayOfWeek in hverdagene

        fun Periode.antallHverdager(): Hverdager {
            return Hverdager(this.antallDager(*hverdagene.toTypedArray()))
        }

        private fun hverdagerFraOgMed(start: LocalDate): Sequence<LocalDate> {
            var dag = start
            return sequence {
                while (true) {
                    if (dag.erHverdag) {
                        yield(dag)
                    }
                    dag = dag.plusDays(1)
                }
            }
        }
    }
}

enum class Sykdomskvoter(val avslagsårsak: VarighetVurdering.Avslagsårsak) {
    STANDARD(VarighetVurdering.Avslagsårsak.STANDARDKVOTE_BRUKT_OPP),
    STUDENT(VarighetVurdering.Avslagsårsak.STUDENTKVOTE_BRUKT_OPP),
    ETABLERINGSFASE(VarighetVurdering.Avslagsårsak.ETABLERINGSFASEKVOTE_BRUKT_OPP),
    UTVIKLINGSFASE(VarighetVurdering.Avslagsårsak.UTVIKLINGSFASEKVOTE_BRUKT_OPP),
}

data class KvoteTilstand(
    val type: Sykdomskvoter,
    private val kvote: Hverdager,
    private var kvoteBrukt: Hverdager = Hverdager(0),
) {
    var erKvoteOversteget: Boolean = false
        set(erKvoteOversteget: Boolean) {
            require(erKvoteOversteget)
            field = erKvoteOversteget
        }

    val ubruktKvote: Hverdager
        get() = kvote - kvoteBrukt

    fun øk(brukt: Hverdager) {
        kvoteBrukt += brukt
    }
}

class Telleverk private constructor(
    private val standardkvote: KvoteTilstand,
    private val studentkvote: KvoteTilstand,
    private val utviklingsfasekvote: KvoteTilstand,
    private val etableringsfasekvote: KvoteTilstand,
) {
    constructor(kvoter: Kvoter) : this(
        standardkvote = KvoteTilstand(
            type = Sykdomskvoter.STANDARD,
            kvote = kvoter.standardkvote,
        ),
        studentkvote = KvoteTilstand(
            type = Sykdomskvoter.STUDENT,
            kvote = kvoter.studentkvote,
        ),
        utviklingsfasekvote = KvoteTilstand(
            type = Sykdomskvoter.UTVIKLINGSFASE,
            kvote = Hverdager(0),
        ),
        etableringsfasekvote = KvoteTilstand(
            type = Sykdomskvoter.ETABLERINGSFASE,
            kvote = Hverdager(0),
        ),
    )

    private fun <T> map(relevanteKvoter: Set<Sykdomskvoter>, action: (KvoteTilstand) -> T): List<T> {
        return listOfNotNull(
            if (Sykdomskvoter.STANDARD in relevanteKvoter) action(standardkvote) else null,
            if (Sykdomskvoter.STUDENT in relevanteKvoter) action(studentkvote) else null,
            if (Sykdomskvoter.ETABLERINGSFASE in relevanteKvoter) action(etableringsfasekvote) else null,
            if (Sykdomskvoter.UTVIKLINGSFASE in relevanteKvoter) action(utviklingsfasekvote) else null,
        )
    }

    fun markereKvoterOversteget(kvoterSomSkalMarkeres: Set<Sykdomskvoter>) {
        map(kvoterSomSkalMarkeres) {
            it.erKvoteOversteget = true
        }
    }

    fun kvoterSomErStanset(relevanteKvoter: Set<Sykdomskvoter>): List<Sykdomskvoter> {
        return map(relevanteKvoter) { it }.filter { it.erKvoteOversteget }.map { it.type }
    }

    fun erKvoterStanset(relevanteKvoter: Set<Sykdomskvoter>): Boolean {
        return map(relevanteKvoter) { it.erKvoteOversteget }.any { it }
    }

    fun minsteUbrukteKvote(relevanteKvoter: Set<Sykdomskvoter>): Hverdager {
        return map(relevanteKvoter) { it.ubruktKvote }.min()
    }

    fun kvoterNærmestÅBliBruktOpp(relevanteKvoter: Set<Sykdomskvoter>): Set<Sykdomskvoter> {
        return map(relevanteKvoter) { it }
            .groupBy { it.ubruktKvote }
            .minByOrNull { it.key }
            ?.value
            ?.map { it.type }
            ?.toSet().orEmpty()
    }

    fun øk(relevanteKvoter: Set<Sykdomskvoter>, hverdagerBrukt: Hverdager) {
        map(relevanteKvoter) {
            it.øk(hverdagerBrukt)
        }
    }

    fun øk(relevanteKvoter: Set<Sykdomskvoter>, periode: Periode) {
        øk(relevanteKvoter, periode.antallHverdager())
    }
}