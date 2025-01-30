package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ÅrsakTilBehandling

public sealed interface AnnetRelevantDokument : Melding

@JsonIgnoreProperties(ignoreUnknown = true)
public data class AnnetRelevantDokumentV0(
    public val årsakTilBehandling: ÅrsakTilBehandling,
) : AnnetRelevantDokument
