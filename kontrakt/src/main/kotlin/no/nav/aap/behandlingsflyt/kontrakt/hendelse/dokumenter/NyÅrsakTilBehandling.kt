package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ÅrsakTilBehandling

public sealed interface NyÅrsakTilBehandling : Melding

@JsonIgnoreProperties(ignoreUnknown = true)
public data class NyÅrsakTilBehandlingV0(
    public val årsakerTilBehandling: List<ÅrsakTilBehandling>,
) : NyÅrsakTilBehandling
