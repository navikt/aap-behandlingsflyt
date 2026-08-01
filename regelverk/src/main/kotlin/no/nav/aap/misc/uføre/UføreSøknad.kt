package no.nav.aap.misc.uføre

import java.time.LocalDate

data class UføreSøknad(
    val soknadsdato: LocalDate,
    val sakId: Long
)