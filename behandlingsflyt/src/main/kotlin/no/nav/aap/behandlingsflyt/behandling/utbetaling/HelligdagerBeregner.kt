package no.nav.aap.behandlingsflyt.behandling.utbetaling

import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatter
import kotlin.math.*

object HelligdagerBeregner {

    fun helligdager(year: Year): Set<LocalDate> {
        val easterDay = easterDay(year)
        return setOf(
            LocalDate.of(year.value, 1, 1),         // 1. nyttårsdag
            easterDay.minusDays(3),                 // skjærtorsdag
            easterDay.minusDays(2),                 // langfredag
            easterDay,                              // 1. påskedag
            easterDay.plusDays(1),                  // 2. påskedag
            easterDay.plusDays(39),                 // kristi himmelfartsdag
            easterDay.plusDays(49),                 // 1. pinsedag
            easterDay.plusDays(50),                 // 2. pinsedag
            LocalDate.of(year.value, 5, 1),         // arbeidernes internasjonale kampdag
            LocalDate.of(year.value, 5, 17),        // nasjonaldag
            LocalDate.of(year.value, 12, 25),       // 1. juledag
            LocalDate.of(year.value, 12, 26),       // 2. juledag
        )
    }

    // Meeus/Jones/Butchers formula
    private fun easterDay(year: Year): LocalDate {
        val yearAsInt = year.value
        val a = yearAsInt % 19
        val b = floor(yearAsInt / 100.0)
        val c = yearAsInt % 100
        val d = floor(b / 4)
        val e = b % 4
        val f = floor((b + 8) / 25)
        val g = floor((b - f + 1) / 3)
        val h = (19 * a + b - d - g + 15) % 30
        val i = floor(c / 4.0)
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = floor((a + 11 * h + 22 * l)/451)
        val month = floor((h + l - 7 * m + 114) / 31)
        val dayOfMonth = ((h + l - 7 * m + 114) % 31) + 1

        return LocalDate.parse(
            "${dayOfMonth.toInt().twoDigits()}-${month.toInt().twoDigits()}-$yearAsInt",
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
        )
    }

    private fun Int.twoDigits() = if (this < 10) "0$this" else this.toString()

}