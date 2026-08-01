package no.nav.aap.behandlingsflyt.steg.samordning.barnepensjon

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.samordning.barnepensjon.BarnepensjonGrunnlag
import no.nav.aap.samordning.barnepensjon.BarnepensjonVurdering

interface BarnepensjonRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurdering: BarnepensjonVurdering)
    fun hentHvisEksisterer(behandlingId: BehandlingId): BarnepensjonGrunnlag?
    fun hentHistoriskeVurderinger(sakId: SakId, behandlingId: BehandlingId): List<BarnepensjonGrunnlag>
    fun deaktiverGrunnlag(behandlingId: BehandlingId)
}