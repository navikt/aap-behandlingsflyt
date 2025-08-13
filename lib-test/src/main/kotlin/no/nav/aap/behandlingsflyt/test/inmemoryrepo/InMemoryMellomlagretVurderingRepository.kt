package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurdering
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryMellomlagretVurderingRepository : MellomlagretVurderingRepository {
    private val vurderinger = ConcurrentHashMap<Pair<BehandlingId, String>, MellomlagretVurdering>()
    private val lock = Object()

    override fun hentHvisEksisterer(
        behandlingId: BehandlingId,
        avklaringsbehovKode: String
    ): MellomlagretVurdering? {
        synchronized(lock) { return vurderinger[Pair(behandlingId, avklaringsbehovKode)] }
    }

    override fun lagre(mellomlagretVurdering: MellomlagretVurdering) {
        val (behandlingId, avklaringsbehovKode) = mellomlagretVurdering
        synchronized(lock) { vurderinger[Pair(behandlingId, avklaringsbehovKode)] = mellomlagretVurdering }
    }
}