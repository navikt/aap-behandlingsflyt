package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.type.Periode
import java.time.DayOfWeek
import java.time.LocalDate

@JvmInline
value class Hverdager(val asInt: Int) : Comparable<Hverdager> {
    operator fun plus(other: Hverdager) = Hverdager(this.asInt + other.asInt)
    operator fun minus(other: Hverdager) = Hverdager(this.asInt - other.asInt)
    override fun compareTo(other: Hverdager) = asInt.compareTo(other.asInt)

    companion object {
        fun LocalDate.plusHverdager(hverdager: Hverdager): LocalDate {
            return hverdagerFraOgMed(this).elementAt(hverdager.asInt)
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