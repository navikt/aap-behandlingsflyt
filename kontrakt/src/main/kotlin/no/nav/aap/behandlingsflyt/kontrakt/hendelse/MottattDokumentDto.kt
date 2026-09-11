package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import java.time.LocalDateTime

public data class MottattDokumentDto(
    val type: InnsendingType,
    val referanse: InnsendingReferanse,
    val mottattTidspunkt: LocalDateTime?,
)