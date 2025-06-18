package no.nav.aap.behandlingsflyt.behandling.utbetaling

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Year

sealed interface ØkonomisystemetTilstand
data class Stengt(val åpner: LocalDateTime): ØkonomisystemetTilstand
object Åpent: ØkonomisystemetTilstand

object ØkonomisystemetÅpningstider {

    private val ÅPNINGSTID_MORGEN = LocalTime.of(6, 0)
    private val STENGETID_KVELD = LocalTime.of(21, 0)

    fun sjekk(tid: LocalDateTime = LocalDateTime.now()) : ØkonomisystemetTilstand {
        if (tid.stengt()) {
            return Stengt(tid.nesteÅpningstid())
        }
        return Åpent
    }

    private fun LocalDateTime.nesteÅpningstid(): LocalDateTime {
        if (!this.stengt()) {
            return this
        }
        return if (this.toLocalTime().isBefore(ÅPNINGSTID_MORGEN)) {
            if (this.with(ÅPNINGSTID_MORGEN).stengt()) {
                this.with(ÅPNINGSTID_MORGEN).plusDays(1).nesteÅpningstid()
            } else {
                this.with(ÅPNINGSTID_MORGEN)
            }
        } else {
            this.with(ÅPNINGSTID_MORGEN).plusDays(1).nesteÅpningstid()
        }
    }

    private fun LocalDateTime.stengt(): Boolean {
        val idag = this.toLocalDate()
        val helligdager = HelligdagerBeregner.helligdager(Year.of(this.year))
        if (idag in helligdager || idag.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
            return true
        }
        val tidPåDagen = this.toLocalTime()
        return tidPåDagen.isBefore(ÅPNINGSTID_MORGEN) || tidPåDagen.isAfter(STENGETID_KVELD)
    }


}