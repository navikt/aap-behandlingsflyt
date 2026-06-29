package no.nav.aap.behandlingsflyt.hendelse.datadeling

import java.math.BigDecimal
import java.time.LocalDate

data class UnderveisperiodeDatadeling(
    val fom: LocalDate,
    val tom: LocalDate,
    val meldepliktstatus: String?,
    val arbeidsgrad: Int,
    val overgrenseVerdi: Boolean,
    val timerArbeidet: BigDecimal,
)
