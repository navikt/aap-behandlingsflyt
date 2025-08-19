package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

data class AvklaringsbehovForSak(val behandlingId: BehandlingId, val avklaringsbehov: List<Avklaringsbehov>)