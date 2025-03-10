package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryTilkjentYtelseRepository : TilkjentYtelseRepository {
    private val tilkjentYtelse = mutableMapOf<BehandlingId, List<TilkjentYtelsePeriode>>()
    override fun hentHvisEksisterer(behandlingId: BehandlingId): List<TilkjentYtelsePeriode>? {
        return tilkjentYtelse[behandlingId]
    }

    override fun lagre(behandlingId: BehandlingId, tilkjent: List<TilkjentYtelsePeriode>) {
        tilkjentYtelse[behandlingId] = tilkjent
    }
}