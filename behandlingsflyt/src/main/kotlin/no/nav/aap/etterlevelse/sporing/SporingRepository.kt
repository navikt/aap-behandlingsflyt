package no.nav.aap.etterlevelse.sporing

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface SporingRepository : Repository {
    fun lagre(sporing: Sporing)
    fun hent(behandlingId: BehandlingId, sporingsnøkkel: Sporingsnøkkel): Sporing?
}