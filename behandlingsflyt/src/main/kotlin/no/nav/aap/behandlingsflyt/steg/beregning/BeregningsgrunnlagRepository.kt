package no.nav.aap.behandlingsflyt.steg.beregning

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.beregning.Beregningsgrunnlag
import no.nav.aap.lookup.repository.Repository

interface BeregningsgrunnlagRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): Beregningsgrunnlag?
    fun lagre(behandlingId: BehandlingId, beregningsgrunnlag: Beregningsgrunnlag)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun deaktiver(behandlingId: BehandlingId)
}