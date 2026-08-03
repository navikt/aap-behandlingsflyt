package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("MigreringFraArenaV0")
public data class MigreringFraArenaV0(
    public val beskrivelse: String,
) : Melding
