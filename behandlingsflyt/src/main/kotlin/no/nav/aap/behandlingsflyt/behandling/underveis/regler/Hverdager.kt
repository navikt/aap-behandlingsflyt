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

        /**
         * Skal bare legge til 260 dager første året ettersom Kelvin inkluderer startdato og dermed blir startdato + 260 dager = 261 totalt.
         * For resterende år er startdato inkludert i forrige periode og det skal legges til riktig antall dagerskal inkludere startdato og derfor får 261 totalt
         **/
        fun LocalDate.plussEtÅrMedHverdager(årMedHverdager: ÅrMedHverdager): LocalDate {
            return when(årMedHverdager) {
                ÅrMedHverdager.FØRSTE_ÅR -> hverdagerFraOgMed(this).elementAt(årMedHverdager.hverdagerIÅret.asInt - 1)
                ÅrMedHverdager.ANDRE_ÅR ,
                ÅrMedHverdager.TREDJE_ÅR ,
                ÅrMedHverdager.ANNET -> hverdagerFraOgMed(this).elementAt(årMedHverdager.hverdagerIÅret.asInt)
            }
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

    fun fraOgMed(start: LocalDate): LocalDate {
        val sisteHverdag = hverdagerFraOgMed(start).elementAt(this.asInt - 1)
        /** Dette bevarer adferden som er implementert for kvoter,  altså at vi velger
         * siste dag før kvoten er brukt opp.
         **/
        return if (sisteHverdag.dayOfWeek == DayOfWeek.FRIDAY) {
            sisteHverdag.plusDays(2)
        } else {
            sisteHverdag
        }
    }
}

/**
 * Antall mandag-fredager per år er bestemt til å være 261 + 261 + 262 for at kvoten skal bli riktig.
 * https://confluence.adeo.no/spaces/PAAP/pages/739025519/Kvoter+og+overganger+mellom+bestemmelser
 */
enum class ÅrMedHverdager(val hverdagerIÅret: Hverdager){
    FØRSTE_ÅR(Hverdager(261)),
    ANDRE_ÅR(Hverdager(261)),
    TREDJE_ÅR(Hverdager(262)),
    ANNET(Hverdager(261))
}

