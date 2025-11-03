package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface PersonopplysningRepository : Repository {
    fun hentBrukerPersonOpplysningHvisEksisterer(behandlingId: BehandlingId): Personopplysning?
    fun lagre(behandlingId: BehandlingId, personopplysning: Personopplysning)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}

