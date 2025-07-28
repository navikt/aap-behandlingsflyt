package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface PersonopplysningRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): PersonopplysningGrunnlag?
    fun hentBrukerPersonOpplysningHvisEksisterer(behandlingId: BehandlingId): Personopplysning?
    fun lagre(behandlingId: BehandlingId, personopplysning: Personopplysning)
    fun lagre(behandlingId: BehandlingId, barn: Set<Barn>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}