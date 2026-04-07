package no.nav.aap.behandlingsflyt.kontrakt.oppgave

public data class EnhetNrDto(
    val enhetNr: String,
)

public data class EnhetForPersonRequest(
    val personIdent: String,
    val relevanteIdenter: List<String>,
)

