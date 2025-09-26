package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import java.util.UUID

public sealed interface NyÅrsakTilBehandling : Melding

@JsonIgnoreProperties(ignoreUnknown = true)
public data class NyÅrsakTilBehandlingV0(
    public val årsakerTilBehandling: List<Vurderingsbehov>,
    public val behandlingReferanse: String,
) : NyÅrsakTilBehandling
