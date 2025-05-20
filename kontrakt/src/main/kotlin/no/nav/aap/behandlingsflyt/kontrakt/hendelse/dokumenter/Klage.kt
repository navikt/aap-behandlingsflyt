package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ÅrsakTilBehandling
import java.time.LocalDate

public sealed interface Klage : Melding

@JsonIgnoreProperties(ignoreUnknown = true)
public data class KlageV0(
    public val kravMottatt: LocalDate,
    public val skalOppretteNyBehandling: Boolean? = true
) : Klage
