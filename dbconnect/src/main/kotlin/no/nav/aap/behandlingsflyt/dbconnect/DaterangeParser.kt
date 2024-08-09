package no.nav.aap.behandlingsflyt.dbconnect

import no.nav.aap.verdityper.Periode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal object DaterangeParser {

    private val formater = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    internal fun toSQL(periode: Periode): String {
        return "[${formatSingleDate(periode.fom)},${formatSingleDate(periode.tom)}]"
    }

    internal fun fromSQL(daterange: String): Periode {
        val (lower, upper) = daterange.split(",")

        val fom = fomFromSql(lower)
        var tom = tomFromSql(upper)

        return Periode(fom, tom)
    }

    private fun fomFromSql(lower: String): LocalDate {
        val lowerEnd = lower.first()
        val lowerDate = lower.drop(1)

        if (lowerDate.isEmpty()) return LocalDate.MIN

        var fom = formater.parse(lowerDate, LocalDate::from)
        if (lowerEnd == '(') {
            fom = fom.plusDays(1)
        }
        return fom
    }

    private fun tomFromSql(upper: String): LocalDate {
        val upperDate = upper.dropLast(1)
        val upperEnd = upper.last()

        if (upperDate.isEmpty()) return LocalDate.MAX

        var tom = formater.parse(upperDate, LocalDate::from)
        if (upperEnd == ')') {
            tom = tom.minusDays(1)
        }
        return tom
    }

    private fun formatSingleDate(date: LocalDate): String =
        if (date == LocalDate.MAX || date == LocalDate.MIN) ""
        else formater.format(date)
}
