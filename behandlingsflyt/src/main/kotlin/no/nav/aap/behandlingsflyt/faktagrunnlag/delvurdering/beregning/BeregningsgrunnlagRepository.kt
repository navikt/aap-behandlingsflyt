package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface BeregningsgrunnlagRepository : Repository{
    fun hentHvisEksisterer(behandlingId: BehandlingId): Beregningsgrunnlag?
    fun lagre(behandlingId: BehandlingId, beregningsgrunnlag: Beregningsgrunnlag)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun deaktiver(behandlingId: BehandlingId)
}