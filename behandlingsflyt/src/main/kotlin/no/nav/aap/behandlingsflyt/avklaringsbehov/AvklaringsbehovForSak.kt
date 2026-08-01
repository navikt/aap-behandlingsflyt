package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandling.BehandlingId

data class AvklaringsbehovForSak(val behandlingId: BehandlingId, val avklaringsbehov: List<Avklaringsbehov>)