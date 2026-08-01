package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface AvklaringsbehovRepository : Repository {
    fun hentAvklaringsbehovene(behandlingId: BehandlingId): Avklaringsbehovene
}