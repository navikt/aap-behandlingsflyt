package no.nav.aap.behandlingsflyt.utils

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun Collection<Periode>.toHumanReadable(): String {
    val lesbarePerioder = this.map { it.toHumanReadable() }

    return when(lesbarePerioder.size) {
        0 -> ""
        1 -> lesbarePerioder.first()
        else -> {
            val siste = lesbarePerioder.last()
            val alleUtenomSiste = lesbarePerioder.dropLast(1)
            "${alleUtenomSiste.joinToString()} og $siste"
        }
    }
}

fun Periode.toHumanReadable() =
    "${fom.tilNorsktFormat()}–${tom.tilNorsktFormat()}"

fun LocalDate?.tilNorsktFormat(): String {
    if (this == null || !isBefore(Tid.MAKS)) {
        return "løpende"
    }
    return format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}