package no.nav.aap.behandlingsflyt.kontrakt.hendelse

public data class MottattDokumentDto(
    val type: InnsendingType,
    val referanse: InnsendingReferanse,
)