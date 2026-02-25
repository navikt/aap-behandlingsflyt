package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface StansOpphørRepository: Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): StansOpphørGrunnlag?
    fun lagre(behandlingId: BehandlingId, grunnlag: StansOpphørGrunnlag)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}