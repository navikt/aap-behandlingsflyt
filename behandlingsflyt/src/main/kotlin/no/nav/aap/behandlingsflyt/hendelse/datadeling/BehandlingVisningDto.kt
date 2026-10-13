package no.nav.aap.behandlingsflyt.hendelse.datadeling

import no.nav.aap.behandlingsflyt.datadeling.BehandlingVisning

data class BehandlingVisningDto (
    val ident: String,
    val behandlinger: List<BehandlingVisning>
)