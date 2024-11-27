package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.dokument.Kanal

public data class MottattHendelseDto(
    val saksnummer: Saksnummer,
    val type: InnsendingType,
    val kana: Kanal,
    val hendelseId: InnsendingReferanse,
    val payload: Any?
)