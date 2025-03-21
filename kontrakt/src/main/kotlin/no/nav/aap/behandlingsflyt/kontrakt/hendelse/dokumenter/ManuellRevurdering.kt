package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ÅrsakTilBehandling

public sealed interface ManuellRevurdering : Melding

@JsonIgnoreProperties(ignoreUnknown = true)
public data class ManuellRevurderingV0(
    public val årsakerTilBehandling: List<ÅrsakTilBehandling>,
    public val beskrivelse: String,
) : ManuellRevurdering
