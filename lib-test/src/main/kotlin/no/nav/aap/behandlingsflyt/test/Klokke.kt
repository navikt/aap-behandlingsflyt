package no.nav.aap.behandlingsflyt.test

import java.time.Clock
import java.time.LocalDate

fun fixedClock(dato: LocalDate, zoneId: String = "Europe/Oslo") =
    Clock.fixed(dato.atStartOfDay().atZone(java.time.ZoneId.of(zoneId)).toInstant(), java.time.ZoneId.of(zoneId))