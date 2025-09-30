package no.nav.aap.behandlingsflyt.behandling.oppholdskrav

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface OppholdskravGrunnlagRepository : Repository {

    fun hentHvisEksisterer(behandlingId: BehandlingId): OppholdskravGrunnlag?
    fun lagre(behandlingId: BehandlingId, oppholdskravVurdering: OppholdskravVurdering)
    fun tilbakestillGrunnlag(behandlingId: BehandlingId, forrigeBehandling: BehandlingId?)


}