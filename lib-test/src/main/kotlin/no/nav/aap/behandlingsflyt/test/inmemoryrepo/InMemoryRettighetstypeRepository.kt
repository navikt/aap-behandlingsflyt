package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeFaktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

object InMemoryRettighetstypeRepository : RettighetstypeRepository {
    private val data = ConcurrentHashMap<BehandlingId, RettighetstypeGrunnlag>()
    private val lock = Any()

    override fun hent(behandlingId: BehandlingId): RettighetstypeGrunnlag =
        hentHvisEksisterer(behandlingId)
            ?: throw NoSuchElementException("Fant ikke rettighetstypegrunnlag for behandlingId=$behandlingId")

    override fun hentHvisEksisterer(behandlingId: BehandlingId): RettighetstypeGrunnlag? =
        synchronized(lock) { data[behandlingId] }

    override fun lagre(
        behandlingId: BehandlingId,
        rettighetstypeTidslinje: Tidslinje<RettighetsType>,
        faktagrunnlag: RettighetstypeFaktagrunnlag,
        versjon: String
    ) {
        synchronized(lock) {
            val nyttGrunnlag = RettighetstypeGrunnlag(rettighetstypeTidslinje)
            if (data[behandlingId] != nyttGrunnlag) {
                data[behandlingId] = nyttGrunnlag
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            data.remove(behandlingId)
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        synchronized(lock) {
            data[fraBehandling]?.let {
                data[tilBehandling] = it
            }
        }
    }
}
