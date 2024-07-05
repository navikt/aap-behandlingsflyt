package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class AvklaringsbehovHendelseHåndterer(connection: DBConnection) {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)
    private val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(connection)

    fun håndtere(key: BehandlingId, hendelse: LøsAvklaringsbehovBehandlingHendelse) {
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