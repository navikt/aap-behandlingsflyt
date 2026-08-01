package no.nav.aap.behandlingsflyt.steg.sykepengeerstatning

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.sykepengererstatning.SykepengerErstatningGrunnlag
import no.nav.aap.sykepengererstatning.SykepengerVurdering

interface SykepengerErstatningRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurderinger: List<SykepengerVurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hentHvisEksisterer(behandlingId: BehandlingId): SykepengerErstatningGrunnlag?
}