package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InMemoryUnderveisRepository : UnderveisRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, UnderveisGrunnlag>()
    private val id = AtomicLong(0)

    override fun hent(behandlingId: BehandlingId): UnderveisGrunnlag {
        return hentHvisEksisterer(behandlingId)!!
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): UnderveisGrunnlag? {
        return grunnlag[behandlingId]
    }

    override fun lagre(behandlingId: BehandlingId, underveisperioder: List<Underveisperiode>, input: Faktagrunnlag) {
        grunnlag[behandlingId] = UnderveisGrunnlag(
            id = id.getAndIncrement(),
            perioder = underveisperioder,
        )
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
    }
}