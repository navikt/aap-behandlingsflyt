package no.nav.aap.behandlingsflyt.dbconnect

import no.nav.aap.verdityper.Periode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal object DaterangeParser {

    private val formater = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    internal fun toSQL(periode: Periode): String {
        return "[${formater.format(periode.fom)},${formater.format(periode.tom)}]"
    }

    internal fun fromSQL(daterange: String): Periode {
        val (lower, upper) = daterange.split(",")

        val lowerEnd = lower.first()
        val lowerDate = lower.drop(1)
        val upperDate = upper.dropLast(1)
        val upperEnd = upper.last()

        var fom = formater.parse(lowerDate, LocalDate::from)
        if (lowerEnd == '(') {
            fom = fom.plusDays(1)
        }

        var tom = formater.parse(upperDate, LocalDate::from)
        if (upperEnd == ')') {
            tom = tom.minusDays(1)
        }

        return Periode(fom, tom)
    }
}
