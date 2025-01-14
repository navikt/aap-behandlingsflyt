package no.nav.aap.behandlingsflyt.forretningsflyt.steg.effektuer11_7

import java.time.Clock
import java.time.LocalDate

object BruddAktivitetspliktClockImpl : BruddAktivitetspliktClock {
    private val clock = Clock.systemDefaultZone()

    override fun now(): LocalDate {
        return LocalDate.now(clock)
    }
}