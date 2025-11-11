package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InMemorySamordningYtelseRepository : SamordningYtelseRepository {
    private val ytelser = ConcurrentHashMap<BehandlingId, List<Pair<SamordningYtelseGrunnlag, Instant>>>()
    private val lock = Object()
    private val idSeq = AtomicLong(10000)


    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningYtelseGrunnlag? {
        synchronized(lock) {
            return ytelser[behandlingId]?.maxByOrNull { it.second }?.first
        }
    }

    override fun hentEldsteGrunnlag(behandlingId: BehandlingId): SamordningYtelseGrunnlag? {
        synchronized(lock) {
            return ytelser[behandlingId]?.minByOrNull { it.second }?.first
        }
    }

    override fun lagre(behandlingId: BehandlingId, samordningYtelser: Set<SamordningYtelse>) {
        synchronized(lock) {
            if (ytelser.containsKey(behandlingId)) {
                ytelser[behandlingId] = ytelser[behandlingId]!! + Pair(
                    SamordningYtelseGrunnlag(
                        grunnlagId = idSeq.andIncrement,
                        ytelser = samordningYtelser.toSet(),
                    ), Instant.now()
                )
            } else {
                ytelser[behandlingId] = listOf(
                    Pair(
                        SamordningYtelseGrunnlag(
                            grunnlagId = idSeq.andIncrement,
                            ytelser = samordningYtelser.toSet(),
                        ), Instant.now()
                    )
                )
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        synchronized(lock) {
            hentHvisEksisterer(fraBehandling) ?: return

            val fraYtelser = ytelser[fraBehandling]
            if (fraYtelser != null) {
                val aktivYtelse = fraYtelser.maxByOrNull { it.second }
                if (aktivYtelse != null) {
                    if (ytelser.containsKey(tilBehandling)) {
                        ytelser[tilBehandling] = ytelser[tilBehandling]!! + Pair(
                            aktivYtelse.first.copy(grunnlagId = idSeq.andIncrement),
                            Instant.now()
                        )
                    } else {
                        ytelser[tilBehandling] =
                            listOf(Pair(aktivYtelse.first.copy(grunnlagId = idSeq.getAndIncrement()), Instant.now()))
                    }
                }
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
    }
}
