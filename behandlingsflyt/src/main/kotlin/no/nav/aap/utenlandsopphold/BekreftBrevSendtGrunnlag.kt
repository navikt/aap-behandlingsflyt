package no.nav.aap.utenlandsopphold

import java.time.LocalDateTime

data class BekreftBrevSendtBekreftelse(
    val brevSendt: Boolean,
    val bekreftetAv: String,
    val bekreftetTidspunkt: LocalDateTime,
)

data class BekreftBrevSendtGrunnlag(
    val bekreftelse: BekreftBrevSendtBekreftelse,
)
