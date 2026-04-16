package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import java.time.LocalDate

data class UføreSøknad(
    val soknadsdato: LocalDate,
    val sakId: Long
)

data class UføreSøknadRequest(val pid: String)
data class UføreSøknadResponse(val soknad: UføreSøknad?)