package no.nav.aap.behandlingsflyt.test

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAmount

class AdjustableClock(
    private var now: Instant,
) : Clock() {


    fun gåFremITid(mengde: TemporalAmount) {
        now = now.plus(mengde)
    }

    override fun instant(): Instant {
        return now
    }

    override fun withZone(zone: ZoneId?): Clock {
        throw NotImplementedError()
    }

    override fun getZone(): ZoneId {
        return ZoneId.systemDefault()
    }
}