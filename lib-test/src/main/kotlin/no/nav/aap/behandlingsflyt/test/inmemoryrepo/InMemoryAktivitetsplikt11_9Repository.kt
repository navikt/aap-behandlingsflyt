package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Vurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryAktivitetsplikt11_9Repository : Aktivitetsplikt11_9Repository {

    private val memory = HashMap<BehandlingId, Aktivitetsplikt11_9Grunnlag>()
    private val lock = Object()
    override fun hentHvisEksisterer(behandlingId: BehandlingId): Aktivitetsplikt11_9Grunnlag? {
        return synchronized(lock) {
            memory[behandlingId]
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        vurderinger: Set<Aktivitetsplikt11_9Vurdering>
    ) {
        return synchronized(lock) {
            memory[behandlingId] = Aktivitetsplikt11_9Grunnlag(vurderinger = vurderinger)
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        return synchronized(lock) {
            memory[fraBehandling]?.let {
                memory[tilBehandling] = it
            }
        }

    }

    override fun slett(behandlingId: BehandlingId) {
    }

}