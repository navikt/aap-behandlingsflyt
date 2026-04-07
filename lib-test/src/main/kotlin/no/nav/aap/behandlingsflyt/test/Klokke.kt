package no.nav.aap.behandlingsflyt.test

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

fun fixedClock(dato: LocalDate, zoneId: String = "Europe/Oslo"): Clock =
    Clock.fixed(dato.atStartOfDay().atZone(ZoneId.of(zoneId)).toInstant(), ZoneId.of(zoneId))