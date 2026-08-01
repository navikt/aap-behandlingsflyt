package no.nav.aap.behandlingsflyt.steg.oppholdskrav

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.oppholdskrav.OppholdskravGrunnlag
import no.nav.aap.oppholdskrav.OppholdskravVurdering

interface OppholdskravGrunnlagRepository : Repository {

    fun hentHvisEksisterer(behandlingId: BehandlingId): OppholdskravGrunnlag?
    fun lagre(behandlingId: BehandlingId, oppholdskravVurdering: OppholdskravVurdering)
    fun tilbakestillGrunnlag(behandlingId: BehandlingId, forrigeBehandling: BehandlingId?)


}