package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.behandlingRepository
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class AvklaringsbehovHendelseHåndterer(connection: DBConnection) {

    private val behandlingRepository = behandlingRepository(connection)
    private val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)
    private val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(connection)

    fun håndtere(key: BehandlingId, hendelse: LøsAvklaringsbehovBehandlingHendelse) {
        val behandling = behandlingRepository.hent(key)

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        avklaringsbehovene.validateTilstand(behandling = behandling)

        avklaringsbehovOrkestrator.løsAvklaringsbehovOgFortsettProsessering(
            kontekst = behandling.flytKontekst(),
            avklaringsbehov = hendelse.behov(),
            ingenEndringIGruppe = hendelse.ingenEndringIGruppe
        )
    }
}