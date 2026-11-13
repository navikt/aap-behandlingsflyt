package no.nav.aap.utenlandsopphold

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface SøknadUtenlandsoppholdRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurdering: SøknadUtenlandsoppholdVurdering)

    fun hentHvisEksisterer(behandlingId: BehandlingId): SøknadUtenlandsoppholdGrunnlag?
}
