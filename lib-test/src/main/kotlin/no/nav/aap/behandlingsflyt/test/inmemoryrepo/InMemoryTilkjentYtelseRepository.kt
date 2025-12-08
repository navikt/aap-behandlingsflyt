package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseGrunnlag
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryTilkjentYtelseRepository : TilkjentYtelseRepository {
    private val tilkjentYtelse = ConcurrentHashMap<BehandlingId, List<TilkjentYtelsePeriode>>()
    private val lock = Object()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): List<TilkjentYtelsePeriode>? {
        synchronized(lock) {
            return tilkjentYtelse[behandlingId]
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        tilkjent: List<TilkjentYtelsePeriode>,
        faktagrunnlag: TilkjentYtelseGrunnlag,
        versjon: String
    ) {
        synchronized(lock) {
            tilkjentYtelse[behandlingId] = tilkjent
        }
    }

    override fun slett(behandlingId: BehandlingId) {
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
    }
}
