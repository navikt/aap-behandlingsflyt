package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.personopplysninger.PersonopplysningMedHistorikk
import no.nav.aap.personopplysninger.PersonopplysningMedHistorikkGrunnlag

interface PersonopplysningForutgåendeRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): PersonopplysningMedHistorikkGrunnlag?
    fun lagre(behandlingId: BehandlingId, personopplysning: PersonopplysningMedHistorikk)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}