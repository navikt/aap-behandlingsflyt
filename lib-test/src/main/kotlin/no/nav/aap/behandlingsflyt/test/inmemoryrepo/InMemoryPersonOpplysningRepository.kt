package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryPersonOpplysningRepository : PersonopplysningRepository {
    override fun hentHvisEksisterer(behandlingId: BehandlingId): PersonopplysningGrunnlag? {
        return null
    }

    override fun lagre(behandlingId: BehandlingId, personopplysning: Personopplysning) {

    }

    override fun lagre(behandlingId: BehandlingId, barn: Set<Barn>) {
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
    }

    override fun slett(behandlingId: BehandlingId) {
    }
}
