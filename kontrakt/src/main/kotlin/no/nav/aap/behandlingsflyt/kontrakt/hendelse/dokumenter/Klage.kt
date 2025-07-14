package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

public sealed interface Klage : Melding

@JsonIgnoreProperties(ignoreUnknown = true)
public data class KlageV0(
    public val kravMottatt: LocalDate,
    public val skalOppretteNyBehandling: Boolean? = true,
    public val beskrivelse: String = "",
) : Klage
