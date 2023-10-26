package no.nav.aap.behandlingsflyt.dbstuff

import no.nav.aap.behandlingsflyt.Periode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DaterangeParser {

    private val formater = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun toSQL(periode: Periode): String {
        return "[${formater.format(periode.fom)},${formater.format(periode.tom)}]"
    }

    fun fromSQL(collumnValue: String): Periode {
        val splitteRange = collumnValue.split(",")
        val fomString = splitteRange[0]
        val tomString = splitteRange[1]

        val fom = parseLower(fomString)
        val tom = parseUpper(tomString)
        return Periode(fom, tom)
    }

    private fun parseLower(dateString: String): LocalDate {
        var inclusive = true
        var cleanedString = dateString
        if (dateString.contains("(")) {
            inclusive = false
            cleanedString = cleanedString.replace("(", "")
        } else {
            cleanedString = cleanedString.replace("[", "")
        }
        val parsedDate = formater.parse(cleanedString, LocalDate::from)!!
        if (!inclusive) {
            return parsedDate.plusDays(1)
        }
        return parsedDate
    }

    private fun parseUpper(dateString: String): LocalDate {
        var inclusive = true
        var cleanedString = dateString
        if (dateString.contains(")")) {
            inclusive = false
            cleanedString = cleanedString.replace(")", "")
        } else {
            cleanedString = cleanedString.replace("]", "")
        }
        val parsedDate = formater.parse(cleanedString, LocalDate::from)!!
        if (!inclusive) {
            return parsedDate.minusDays(1)
        }
        return parsedDate
    }
}