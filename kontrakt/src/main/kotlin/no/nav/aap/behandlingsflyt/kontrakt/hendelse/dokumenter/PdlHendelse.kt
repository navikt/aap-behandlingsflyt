package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.util.UUID

public sealed interface PdlHendelse : Melding

@JsonIgnoreProperties(ignoreUnknown = true)
public data class PdlHendelseKafkaMelding(
    val eventId: UUID,
    val kildeReferanse: String,
    val kilde: String,
) {

    public fun tilPdlHendelseV0(): PdlHendelse =
        PdlHendelseV0(
            eventId = eventId,
            kildeReferanse = kildeReferanse,
            kilde = kilde,
        )
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class PdlHendelseV0(
    val eventId: UUID,
    val kildeReferanse: String,
    val kilde: String,
) : PdlHendelse



