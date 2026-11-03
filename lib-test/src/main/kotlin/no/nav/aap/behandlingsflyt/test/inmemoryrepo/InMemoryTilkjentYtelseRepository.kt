package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentForRedusertDagsatsBackfill
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseGrunnlag
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InMemoryTilkjentYtelseRepository : TilkjentYtelseRepository {
    private data class LagretTilkjentYtelse(
        val id: Long,
        val periode: TilkjentYtelsePeriode,
    )

    private val tilkjentYtelse = ConcurrentHashMap<BehandlingId, List<LagretTilkjentYtelse>>()
    private val tilkjentPeriodeId = AtomicLong()
    private val lock = Any()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): List<TilkjentYtelsePeriode>? {
        synchronized(lock) {
            return tilkjentYtelse[behandlingId]?.map { it.periode }
        }
    }

    override fun hentTilkjentUtenRedusertDagsats(behandlingId: BehandlingId): List<TilkjentForRedusertDagsatsBackfill> {
        synchronized(lock) {
            return tilkjentYtelse[behandlingId].orEmpty()
                .filter { it.periode.tilkjent.redusertDagsatsOrNull() == null }
                .map { lagret ->
                    val tilkjent = lagret.periode.tilkjent
                    TilkjentForRedusertDagsatsBackfill(
                        tilkjentPeriodeId = lagret.id,
                        dagsats = tilkjent.dagsats,
                        barnetillegg = tilkjent.barnetillegg,
                        barnepensjonDagsats = tilkjent.barnepensjonDagsats,
                        gradering = tilkjent.gradering,
                        redusertDagsats = tilkjent.redusertDagsatsOrNull()
                    )
                }
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        tilkjent: List<TilkjentYtelsePeriode>,
        faktagrunnlag: TilkjentYtelseGrunnlag,
        versjon: String
    ) {
        synchronized(lock) {
            tilkjentYtelse[behandlingId] = tilkjent.map {
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
                        val periode = eksisterende.periode
                        val oppdatertTilkjent = periode.tilkjent.copy(
                            redusertDagsats = kandidat.redusertDagsats
                        )
                        val oppdatertPeriode = periode.copy(tilkjent = oppdatertTilkjent)
                        eksisterende.copy(periode = oppdatertPeriode)
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
