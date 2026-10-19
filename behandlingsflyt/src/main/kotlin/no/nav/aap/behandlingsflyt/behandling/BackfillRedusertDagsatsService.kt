package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(BackfillRedusertDagsatsService::class.java)

class BackfillRedusertDagsatsService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val kravRepository: KravRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val rettighetsperiodeRepository: VurderRettighetsperiodeRepository,
    private val trukketSøknadService: TrukketSøknadService,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        tilkjentYtelseRepository = repositoryProvider.provide(),
        kravRepository = repositoryProvider.provide(),
        stønadsperiodeRepository = repositoryProvider.provide(),
        mottattDokumentRepository = repositoryProvider.provide(),
        rettighetsperiodeRepository = repositoryProvider.provide(),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
    )

    fun erTrukketSøknadSak(behandlinger: List<Behandling>): Boolean =
        behandlinger.filter { it.status().erAvsluttet() }.any { trukketSøknadService.søknadErTrukket(it.id) }

    /**
     * Backfiller redusertDagsats for én behandling.
     *
     * Returnerer [BackfillBehandlingResultat.AlleredeBackfilled] dersom behandlingen allerede hadde redusertDagsats –
     * løkken i runner skal da bryte ut av saken, da resten er nyere og allerede har sine vurderinger.
     */
    fun backfillBehandling(sak: Sak, behandlinger: List<Behandling>, behandling: Behandling, erNyesteBehandling: Boolean): BackfillBehandlingResultat {
        // TODO: 1. finn alle tilkjentYtelsePerioder som mangler redusertDagsats
        val tilkjentBackfillKandidater = tilkjentYtelseRepository.hentTilkjentUtenRedusertDagsats(behandling.id)
        if (tilkjentBackfillKandidater.isEmpty()) {
            return BackfillBehandlingResultat.AlleredeBackfilled
        }

        // TODO: 2. beregn redusertDagsats (som i TilkjentYtelseService)
        for (tilkjent in tilkjentBackfillKandidater) {
            check(tilkjent.redusertDagsats == null)
            tilkjent.redusertDagsats = tilkjent.beregnRedusertDagsats()
        }

        // TODO: 3. lagre(redusertDagsats)
        tilkjentYtelseRepository.backfillRedusertDagsats(tilkjentBackfillKandidater)

        // TODO 3.1 sjekk om batch har oppdatert samtlige rader som manglet redusertDagsats
        // DBConnection batch returnerer ikke antallRader som er endra, så blir en ekstra spørring her for å sjekke
        val gjenståendeKandidater = tilkjentYtelseRepository.hentTilkjentUtenRedusertDagsats(behandling.id)
        if (gjenståendeKandidater.isNotEmpty()) {
            log.warn("Backfill av redusertDagsats er gjennomført, men X antall kandidater ble ikke oppdatert.")
        }

        // TODO: 4. rydd vekk irrelevant krav-logikk

        return BackfillBehandlingResultat.Backfilled
    }
}