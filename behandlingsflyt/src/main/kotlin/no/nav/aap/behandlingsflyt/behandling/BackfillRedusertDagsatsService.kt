package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(BackfillRedusertDagsatsService::class.java)

class BackfillRedusertDagsatsService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        tilkjentYtelseRepository = repositoryProvider.provide(),
    )

    /**
     * Backfiller redusertDagsats for én behandling.
     */
    fun backfillBehandling(behandlingId: BehandlingId): Int {
        var iderMedBackfillUtført = emptyList<Long>()
        val backfillKandidater = tilkjentYtelseRepository.hentTilkjentUtenRedusertDagsats(behandlingId)
        if (backfillKandidater.isEmpty()) {
            return iderMedBackfillUtført.size
        }

        for (tilkjent in backfillKandidater) {
            check(tilkjent.redusertDagsats == null)
            tilkjent.redusertDagsats = tilkjent.beregnRedusertDagsats()
        }
        tilkjentYtelseRepository.backfillRedusertDagsats(backfillKandidater)

        val gjenstående = tilkjentYtelseRepository.hentTilkjentUtenRedusertDagsats(behandlingId)
        if (gjenstående.isNotEmpty()) {
            iderMedBackfillUtført = backfillKandidater.map { it.tilkjentPeriodeId } - gjenstående.map { it.tilkjentPeriodeId }
            log.warn("Backfill av redusertDagsats for $behandlingId ble gjennomført for ${iderMedBackfillUtført.size} tilkjent_periode rader, men ${gjenstående.size} rader gjenstår.")
        } else {
            iderMedBackfillUtført = backfillKandidater.map { it.tilkjentPeriodeId }
        }

        log.info("Backfill utført for tilkjent_periode radene: ${iderMedBackfillUtført}")
        return iderMedBackfillUtført.size
    }
}