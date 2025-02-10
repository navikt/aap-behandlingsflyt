package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface PersonopplysningForutgåendeRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): PersonopplysningGrunnlag?
    fun lagre(behandlingId: BehandlingId, personopplysning: Personopplysning)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}