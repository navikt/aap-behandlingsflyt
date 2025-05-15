package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklaringsbehovHendelseHåndterer(
    private val avklaringsbehovOrkestrator: AvklaringsbehovOrkestrator,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlingRepository: BehandlingRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(repositoryProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )


    fun håndtere(key: BehandlingId, hendelse: LøsAvklaringsbehovHendelse) {
        val behandling = behandlingRepository.hent(key)

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

        avklaringsbehovene.validateTilstand(
            behandling = behandling,
            avklaringsbehov = hendelse.behov().definisjon()
        )

        avklaringsbehovOrkestrator.løsAvklaringsbehovOgFortsettProsessering(
            kontekst = behandling.flytKontekst(),
            avklaringsbehov = hendelse.behov(),
            ingenEndringIGruppe = hendelse.ingenEndringIGruppe,
            bruker = hendelse.bruker
        )
    }
}