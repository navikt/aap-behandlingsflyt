package no.nav.aap.behandlingsflyt.forretningsflyt.steg.effektuer11_7

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAmount

class FakeBruddAktivitetspliktClock(
    private var clock: Clock
) : BruddAktivitetspliktClock {

    override fun now(): LocalDate {
        return LocalDate.now(clock)
    }

    fun gåFremITid(mengde: TemporalAmount) {
        clock = Clock.fixed(clock.instant().plus(mengde), ZoneId.systemDefault())
    }
}