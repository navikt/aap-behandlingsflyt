package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklaringsbehovHendelseHåndterer(
    private val avklaringsbehovOrkestrator: AvklaringsbehovOrkestrator,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlingRepository: BehandlingRepository,
    private val mellomlagretVurderingRepository: MellomlagretVurderingRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(repositoryProvider, gatewayProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        mellomlagretVurderingRepository = repositoryProvider.provide(),
    )


    fun håndtere(
        behandlingId: BehandlingId,
        avklaringsbehovLøsning: AvklaringsbehovLøsning,
        bruker: Bruker,
    ) {
        val behandling = behandlingRepository.hent(behandlingId)

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

        avklaringsbehovene.validerTilstand(
            behandling = behandling,
            avklaringsbehov = avklaringsbehovLøsning.definisjon()
        )

        avklaringsbehovOrkestrator.løsAvklaringsbehovOgFortsettProsessering(
            kontekst = behandling.flytKontekst(),
            avklaringsbehov = avklaringsbehovLøsning,
            bruker = bruker
        )
        mellomlagretVurderingRepository.slett(behandlingId, avklaringsbehovLøsning.definisjon().kode)
    }
}