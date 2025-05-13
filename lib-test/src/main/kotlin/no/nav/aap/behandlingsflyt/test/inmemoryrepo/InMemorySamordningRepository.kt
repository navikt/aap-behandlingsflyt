package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InMemorySamordningRepository : SamordningRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, SamordningGrunnlag>()
    private val id = AtomicLong(0)

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningGrunnlag? {
        return grunnlag[behandlingId]
    }

    override fun lagre(behandlingId: BehandlingId, samordningPerioder: List<SamordningPeriode>, input: Faktagrunnlag) {
        grunnlag[behandlingId] = SamordningGrunnlag(
            id = id.getAndIncrement(),
            samordningPerioder = samordningPerioder
        )
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
    }
}