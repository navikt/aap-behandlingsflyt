package no.nav.aap.behandlingsflyt.kontrakt.hendelse

data class MottattHendelseDto (
    val saksnummer: String,
    val type: Brevkategori,
    val hendelseId: String,
    val payload: Any?
)