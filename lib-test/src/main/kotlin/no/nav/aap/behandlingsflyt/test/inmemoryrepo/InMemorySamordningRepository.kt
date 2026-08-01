package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.samordning.SamordningGrunnlag
import no.nav.aap.samordning.SamordningPeriode
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.steg.samordning.SamordningRepository
import java.util.concurrent.ConcurrentHashMap

object InMemorySamordningRepository : SamordningRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, SamordningGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningGrunnlag? {
        return grunnlag[behandlingId]
    }

    override fun lagre(behandlingId: BehandlingId, samordningPerioder: Set<SamordningPeriode>, input: Faktagrunnlag) {
        grunnlag[behandlingId] = SamordningGrunnlag(
            samordningPerioder = samordningPerioder
        )
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
        grunnlag.remove(behandlingId)
    }
}