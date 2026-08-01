package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.personopplysninger.Personopplysning

interface PersonopplysningRepository : Repository {
    fun hentBrukerPersonOpplysningHvisEksisterer(behandlingId: BehandlingId): Personopplysning?
    fun lagre(behandlingId: BehandlingId, personopplysning: Personopplysning)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}

