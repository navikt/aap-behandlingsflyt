package no.nav.aap.behandlingsflyt.flyt.testutil

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class InMemorySamordningRepository : SamordningRepository {
    val grunnlag = ConcurrentHashMap<BehandlingId, SamordningGrunnlag>()
    private val id = AtomicLong(0)

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningGrunnlag? {
        return grunnlag[behandlingId]
    }

    override fun lagre(behandlingId: BehandlingId, samordningPerioder: List<SamordningPeriode>) {
        grunnlag[behandlingId] = SamordningGrunnlag(
            id = id.getAndIncrement(),
            samordningPerioder = samordningPerioder
        )
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO("Not yet implemented")
    }
}