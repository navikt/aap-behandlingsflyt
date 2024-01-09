package no.nav.aap.behandlingsflyt.dbtest

import java.time.LocalDate
import java.time.Month

infix fun Int.januar(år: Int) = LocalDate.of(år, Month.JANUARY, this)
infix fun Int.februar(år: Int) = LocalDate.of(år, Month.FEBRUARY, this)
infix fun Int.mars(år: Int) = LocalDate.of(år, Month.MARCH, this)
infix fun Int.april(år: Int) = LocalDate.of(år, Month.APRIL, this)
infix fun Int.mai(år: Int) = LocalDate.of(år, Month.MAY, this)
infix fun Int.juni(år: Int) = LocalDate.of(år, Month.JUNE, this)
infix fun Int.juli(år: Int) = LocalDate.of(år, Month.JULY, this)
infix fun Int.august(år: Int) = LocalDate.of(år, Month.AUGUST, this)
infix fun Int.september(år: Int) = LocalDate.of(år, Month.SEPTEMBER, this)
infix fun Int.oktober(år: Int) = LocalDate.of(år, Month.OCTOBER, this)
infix fun Int.november(år: Int) = LocalDate.of(år, Month.NOVEMBER, this)
infix fun Int.desember(år: Int) = LocalDate.of(år, Month.DECEMBER, this)
