package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid.SamIdOgTpNr
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid.SamIdRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemorySamIdRepository : SamIdRepository {
    private val samIder = ConcurrentHashMap<BehandlingId, List<SamIdOgTpNr>>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): List<SamIdOgTpNr> =
        samIder[behandlingId].orEmpty()

    override fun lagre(behandlingId: BehandlingId, samIdOgTpNr: List<SamIdOgTpNr>) {
        samIder.compute(behandlingId) { _, eksisterende -> eksisterende.orEmpty() + samIdOgTpNr }
    }
}
