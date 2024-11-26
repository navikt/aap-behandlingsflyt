package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import java.util.*

data class MottattHendelseDto (
    val saksnummer: String,
    val type: Brevkode,
    val hendelseId: UUID,
    val payload: Any?
)