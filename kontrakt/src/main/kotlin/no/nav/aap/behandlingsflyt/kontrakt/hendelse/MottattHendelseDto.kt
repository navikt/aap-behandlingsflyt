package no.nav.aap.behandlingsflyt.kontrakt.hendelse

data class MottattHendelseDto (
    val saksnummer: String,
    val type: Brevkode,
    val hendelseId: String,
    val payload: Any?
)