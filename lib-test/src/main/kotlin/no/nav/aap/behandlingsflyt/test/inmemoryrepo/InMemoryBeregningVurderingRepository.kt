package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId

object InMemoryBeregningVurderingRepository : BeregningVurderingRepository {

    private val memory = HashMap<BehandlingId, BeregningGrunnlag>()
    private val lock = Any()

    override fun hent(behandlingId: BehandlingId): BeregningGrunnlag {
        synchronized(lock) {
            return memory[behandlingId] ?: BeregningGrunnlag(null, null)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): BeregningGrunnlag? {
        synchronized(lock) {
            return memory[behandlingId]
        }
    }

    override fun lagre(behandlingId: BehandlingId, vurdering: BeregningstidspunktVurdering?) {
        synchronized(lock) {
            val existing = memory[behandlingId]
            memory[behandlingId] = BeregningGrunnlag(
                tidspunktVurdering = vurdering,
                yrkesskadeBeløpVurdering = existing?.yrkesskadeBeløpVurdering,
            )
        }
    }

    override fun lagre(behandlingId: BehandlingId, vurdering: List<YrkesskadeBeløpVurdering>) {
        // Ikke implementert i minne
    }

    override fun hentHistoriskeVurderinger(sakId: SakId, behandlingId: BehandlingId): List<BeregningGrunnlag> {
        synchronized(lock) {
            return listOfNotNull(memory[behandlingId])
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        synchronized(lock) {
            val grunnlag = memory[fraBehandling]
            if (grunnlag != null) {
                memory[tilBehandling] = grunnlag
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            memory.remove(behandlingId)
        }
    }
}
