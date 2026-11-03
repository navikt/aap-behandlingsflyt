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
        var antallBackfillUtført = 0
        val tilkjentBackfillKandidater = tilkjentYtelseRepository.hentTilkjentUtenRedusertDagsats(behandlingId)
        if (tilkjentBackfillKandidater.isEmpty()) {
            return antallBackfillUtført
        }

        for (tilkjent in tilkjentBackfillKandidater) {
            check(tilkjent.redusertDagsats == null)
            tilkjent.redusertDagsats = tilkjent.beregnRedusertDagsats()
        }
        tilkjentYtelseRepository.backfillRedusertDagsats(tilkjentBackfillKandidater)

        val gjenstående = tilkjentYtelseRepository.hentTilkjentUtenRedusertDagsats(behandlingId)
        antallBackfillUtført = tilkjentBackfillKandidater.size - gjenstående.size
        if (gjenstående.isNotEmpty()) {
            log.warn("Backfill av redusertDagsats for $behandlingId ble gjennomført for $antallBackfillUtført tilkjent_periode rader, men ${gjenstående.size} rader gjenstår.")
        }

        return antallBackfillUtført
    }
}