package no.nav.aap.behandlingsflyt

import java.time.LocalDate
import java.time.Month

internal infix fun Int.januar(år: Int) = LocalDate.of(år, Month.JANUARY, this)
internal infix fun Int.februar(år: Int) = LocalDate.of(år, Month.FEBRUARY, this)
internal infix fun Int.mars(år: Int) = LocalDate.of(år, Month.MARCH, this)
internal infix fun Int.april(år: Int) = LocalDate.of(år, Month.APRIL, this)
internal infix fun Int.mai(år: Int) = LocalDate.of(år, Month.MAY, this)
internal infix fun Int.juni(år: Int) = LocalDate.of(år, Month.JUNE, this)
internal infix fun Int.juli(år: Int) = LocalDate.of(år, Month.JULY, this)
internal infix fun Int.august(år: Int) = LocalDate.of(år, Month.AUGUST, this)
internal infix fun Int.september(år: Int) = LocalDate.of(år, Month.SEPTEMBER, this)
internal infix fun Int.oktober(år: Int) = LocalDate.of(år, Month.OCTOBER, this)
internal infix fun Int.november(år: Int) = LocalDate.of(år, Month.NOVEMBER, this)
internal infix fun Int.desember(år: Int) = LocalDate.of(år, Month.DECEMBER, this)
