package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
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
    fun backfillBehandling(behandling: Behandling): BackfillBehandlingResultat {
        val tilkjentBackfillKandidater = tilkjentYtelseRepository.hentTilkjentUtenRedusertDagsats(behandling.id)
        if (tilkjentBackfillKandidater.isEmpty()) {
            return BackfillBehandlingResultat.Backfilled
        }

        for (tilkjent in tilkjentBackfillKandidater) {
            check(tilkjent.redusertDagsats == null)
            tilkjent.redusertDagsats = tilkjent.beregnRedusertDagsats()
        }
        tilkjentYtelseRepository.backfillRedusertDagsats(tilkjentBackfillKandidater)

        val gjenstående = tilkjentYtelseRepository.hentTilkjentUtenRedusertDagsats(behandling.id)
        if (gjenstående.isNotEmpty()) {
            log.warn("Backfill av redusertDagsats er gjennomført, men ${gjenstående.size} antall kandidater ble ikke oppdatert.")
        }

        return BackfillBehandlingResultat.Backfilled
    }
}