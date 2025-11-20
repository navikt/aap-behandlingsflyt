package no.nav.aap.behandlingsflyt.drift

import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.repository.RepositoryProvider
import org.slf4j.LoggerFactory

/**
 * Klasse for alle driftsfunksjoner. Skal kún brukes av DriftApi.
 * */
class Driftfunksjoner(
    private val behandlingRepository: BehandlingRepository,
    private val taSkriveLåsRepository: TaSkriveLåsRepository,
    private val flytOrkestrator: FlytOrkestrator,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        taSkriveLåsRepository = repositoryProvider.provide(),
        flytOrkestrator = FlytOrkestrator(repositoryProvider, gatewayProvider),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun kjørFraSteg(behandling: Behandling, stegType: StegType) {
        taSkriveLåsRepository.withLåstBehandling(behandling.id) {

            when {
                behandling.status() != Status.UTREDES ->
                    throw UgyldigForespørselException("kan ikke flytte steg i behandling: behandling må ha status ${Status.UTREDES}, men denne behandling har status ${behandling.status()}")

                stegType.status != Status.UTREDES ->
                    throw UgyldigForespørselException("kan ikke flytte behandling til steg $stegType som hører til status ${stegType.status}. Kan kun flytte til steg med status ${Status.UTREDES}")

                stegType !in behandling.flyt().stegene() ->
                    throw UgyldigForespørselException("kan ikke flytte behandling til steg $stegType, da steget ikke hører til flyten ${behandling.typeBehandling()}")

                behandling.flyt().stegComparator.compare(stegType, behandling.aktivtSteg()) > 0 ->
                    throw UgyldigForespørselException("kan ikke flytte behandling til steg $stegType siden steget er etter aktivt steg ${behandling.aktivtSteg()}")
            }

            log.info("setter aktivt steg fra ${behandling.aktivtSteg()} til $stegType")
            behandlingRepository.leggTilNyttAktivtSteg(
                behandling.id, StegTilstand(
                    stegType = stegType,
                    stegStatus = StegStatus.START,
                    aktiv = true,
                )
            )

            flytOrkestrator.prosesserBehandling(flytOrkestrator.opprettKontekst(behandling))
        }
    }
}