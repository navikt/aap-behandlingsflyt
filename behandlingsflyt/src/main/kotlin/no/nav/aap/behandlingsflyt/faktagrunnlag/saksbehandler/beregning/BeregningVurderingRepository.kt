package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.beregning.BeregningGrunnlag
import no.nav.aap.beregning.BeregningstidspunktVurdering
import no.nav.aap.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.lookup.repository.Repository

interface BeregningVurderingRepository : Repository {
    fun hent(behandlingId: BehandlingId): BeregningGrunnlag
    fun lagre(behandlingId: BehandlingId, vurdering: BeregningstidspunktVurdering?)
    fun hentHvisEksisterer(behandlingId: BehandlingId): BeregningGrunnlag?
    fun hentHistoriskeVurderinger(sakId: SakId, behandlingId: BehandlingId): List<BeregningGrunnlag>
    fun lagre(behandlingId: BehandlingId, vurdering: List<YrkesskadeBeløpVurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}