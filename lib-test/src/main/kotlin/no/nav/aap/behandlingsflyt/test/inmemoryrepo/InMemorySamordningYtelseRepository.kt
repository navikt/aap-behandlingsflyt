package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

object InMemorySamordningYtelseRepository : SamordningYtelseRepository {
    private val ytelser = ConcurrentHashMap<BehandlingId, List<Pair<SamordningYtelseGrunnlag, Instant>>>()
    private val lock = Object()

    private var clock = Clock.systemDefaultZone()
    fun setClock(c: Clock) {
        clock = c
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningYtelseGrunnlag? {
        synchronized(lock) {
            return ytelser[behandlingId]?.last()?.first
        }
    }

    override fun hentEldsteGrunnlag(behandlingId: BehandlingId): SamordningYtelseGrunnlag? {
        synchronized(lock) {
            return ytelser[behandlingId]?.first()?.first
        }
    }

    override fun lagre(behandlingId: BehandlingId, samordningYtelser: List<SamordningYtelse>) {
        synchronized(lock) {
            if (ytelser.containsKey(behandlingId)) {
                ytelser[behandlingId] = ytelser[behandlingId]!! + Pair(
                    SamordningYtelseGrunnlag(
                        grunnlagId = behandlingId.id,
                        ytelser = samordningYtelser,
                    ), Instant.now(clock)
                )
            } else {
                ytelser[behandlingId] = listOf(
                    Pair(
                        SamordningYtelseGrunnlag(
                            grunnlagId = behandlingId.id,
                            ytelser = samordningYtelser,
                        ), Instant.now(clock)
                    )
                )
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO()
    }
}