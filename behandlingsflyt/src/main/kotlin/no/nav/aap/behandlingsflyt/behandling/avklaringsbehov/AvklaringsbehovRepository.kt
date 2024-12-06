package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.repository.Repository

interface AvklaringsbehovRepository : Repository {
    fun hentAvklaringsbehovene(behandlingId: BehandlingId): Avklaringsbehovene
}