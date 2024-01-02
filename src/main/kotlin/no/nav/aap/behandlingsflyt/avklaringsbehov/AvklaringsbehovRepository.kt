package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.BehandlingId

interface AvklaringsbehovRepository {
    fun hentAvklaringsbehovene(behandlingId: BehandlingId): Avklaringsbehovene
}