package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurdering
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryMellomlagretVurderingRepository : MellomlagretVurderingRepository {
    private val vurderinger = ConcurrentHashMap<Pair<BehandlingId, AvklaringsbehovKode>, MellomlagretVurdering>()
    private val lock = Object()

    override fun hentHvisEksisterer(
        behandlingId: BehandlingId,
        avklaringsbehovKode: AvklaringsbehovKode
    ): MellomlagretVurdering? {
        synchronized(lock) { return vurderinger[Pair(behandlingId, avklaringsbehovKode)] }
    }

    override fun slett(
        behandlingId: BehandlingId,
        avklaringsbehovKode: AvklaringsbehovKode
    ) {
        synchronized(lock) { vurderinger.remove(Pair(behandlingId, avklaringsbehovKode)) }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            vurderinger.keys.map {
                if (it.first == behandlingId) {
                    vurderinger.remove(it)
                }
            }
        }
    }

    override fun lagre(mellomlagretVurdering: MellomlagretVurdering): MellomlagretVurdering {
        val (behandlingId, avklaringsbehovKode) = mellomlagretVurdering
        synchronized(lock) { vurderinger[Pair(behandlingId, avklaringsbehovKode)] = mellomlagretVurdering }
        return mellomlagretVurdering
    }
}