package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.vedtak.Vedtak
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InMemoryVedtakRepository : VedtakRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, Pair<Long, Vedtak>>()
    private val id = AtomicLong(0)
    override fun lagre(
        behandlingId: BehandlingId,
        vedtakstidspunkt: LocalDateTime,
        virkningstidspunkt: LocalDate?
    ) {
        grunnlag.putIfAbsent(
            behandlingId,
            Pair(id.incrementAndGet(), Vedtak(behandlingId, vedtakstidspunkt, virkningstidspunkt))
        )
    }

    override fun hent(behandlingId: BehandlingId): Vedtak? {
        return grunnlag[behandlingId]?.second
    }

    override fun hentId(behandlingId: BehandlingId): Long {
        return grunnlag[behandlingId]?.first!!
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
        grunnlag.remove(behandlingId)
    }
}