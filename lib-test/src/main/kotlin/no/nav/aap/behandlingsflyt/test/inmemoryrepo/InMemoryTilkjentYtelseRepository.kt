package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentForRedusertDagsatsBackfill
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InMemoryTilkjentYtelseRepository : TilkjentYtelseRepository {
    private data class LagretTilkjentYtelse(
        val id: Long,
        val tilkjent: Tilkjent,
    )

    private val tilkjentYtelse = ConcurrentHashMap<BehandlingId, Tidslinje<LagretTilkjentYtelse>>()
    private val tilkjentPeriodeId = AtomicLong()
    private val lock = Any()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Tidslinje<Tilkjent>? {
        synchronized(lock) {
            return tilkjentYtelse[behandlingId]?.map { it.tilkjent }
        }
    }

    override fun hentTilkjentUtenRedusertDagsats(behandlingId: BehandlingId): List<TilkjentForRedusertDagsatsBackfill> {
        synchronized(lock) {
            return tilkjentYtelse[behandlingId].orEmpty()
                .filter { (_, lagretTilkjent) -> lagretTilkjent.tilkjent.redusertDagsatsOrNull() == null }
                .map { (id, tilkjent) ->
                    TilkjentForRedusertDagsatsBackfill(
                        tilkjentPeriodeId = id,
                        dagsats = tilkjent.dagsats,
                        barnetillegg = tilkjent.barnetillegg,
                        barnepensjonDagsats = tilkjent.barnepensjonDagsats,
                        gradering = tilkjent.gradering,
                        redusertDagsats = tilkjent.redusertDagsatsOrNull()
                    )
                }
                .verdier().toList()
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        resultat: Tidslinje<Tilkjent>,
    ) {
        synchronized(lock) {
            tilkjentYtelse[behandlingId] = resultat.map {
                LagretTilkjentYtelse(tilkjentPeriodeId.incrementAndGet(), it)
            }
        }
    }

    override fun backfillRedusertDagsats(tilkjentBackfillKandidater: List<TilkjentForRedusertDagsatsBackfill>) {
        synchronized(lock) {
            val kandidaterPerId = tilkjentBackfillKandidater.associateBy { it.tilkjentPeriodeId }

            for ((behandlingId, eksisterendeTilkjenteYtelser) in tilkjentYtelse) {
                val oppdatertePerioder = eksisterendeTilkjenteYtelser.map { eksisterende ->
                    val kandidat = kandidaterPerId[eksisterende.id]
                    if (kandidat == null) {
                        eksisterende
                    } else {
                        val oppdatertTilkjent = eksisterende.tilkjent.copy(
                            redusertDagsats = kandidat.redusertDagsats
                        )
                        eksisterende.copy(tilkjent = oppdatertTilkjent)
                    }
                }
                tilkjentYtelse[behandlingId] = oppdatertePerioder
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            tilkjentYtelse.remove(behandlingId)
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
    }
}
