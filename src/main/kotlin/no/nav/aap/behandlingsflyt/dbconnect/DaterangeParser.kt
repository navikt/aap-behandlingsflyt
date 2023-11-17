package no.nav.aap.behandlingsflyt.dbconnect

import no.nav.aap.behandlingsflyt.Periode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DaterangeParser {

    private val formater = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val regex = "([\\[(])(\\d{4}(?:-\\d{2}){2}),(\\d{4}(?:-\\d{2}){2})([])])".toRegex()

    fun toSQL(periode: Periode): String {
        return "[${formater.format(periode.fom)},${formater.format(periode.tom)}]"
    }

    fun fromSQL(daterange: String): Periode {
        val match = regex.matchEntire(daterange)

        requireNotNull(match) { "Daterange-string matcher ikke regex" }

        val lowerEnd = match.groupValues[1]
        val lowerDate = match.groupValues[2]
        val upperDate = match.groupValues[3]
        val upperEnd = match.groupValues[4]

        var fom = formater.parse(lowerDate, LocalDate::from)
        if (lowerEnd == "(") {
            fom = fom.plusDays(1)
        }

        var tom = formater.parse(upperDate, LocalDate::from)
        if (upperEnd == ")") {
            tom = tom.minusDays(1)
        }

        return Periode(fom, tom)
    }
}
