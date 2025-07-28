package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface BarnRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): BarnGrunnlag?
    fun hent(behandlingId: BehandlingId): BarnGrunnlag
    fun lagreOppgitteBarn(behandlingId: BehandlingId, oppgitteBarn: OppgitteBarn?)
    fun lagreRegisterBarn(behandlingId: BehandlingId, barn: List<Ident>)
    fun lagreVurderinger(behandlingId: BehandlingId, vurdertAv: String, vurderteBarn: List<VurdertBarn>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}