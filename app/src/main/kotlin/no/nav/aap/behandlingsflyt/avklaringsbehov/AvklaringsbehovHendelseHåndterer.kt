package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

class AvklaringsbehovHendelseHåndterer(connection: DBConnection) {

    private val behandlingRepository = behandlingRepository(connection)
    private val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)
    private val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(connection)

    fun håndtere(key: BehandlingId, hendelse: LøsAvklaringsbehovBehandlingHendelse) {
        val behandling = behandlingRepository.hent(key)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        ValiderBehandlingTilstand.validerTilstandBehandling(
            behandling = behandling,
            eksisterenedeAvklaringsbehov = avklaringsbehovene.alle()
        )

        val kontekst = behandling.flytKontekst()

        avklaringsbehovOrkestrator.løsAvklaringsbehovOgFortsettProsessering(
            kontekst = kontekst,
            avklaringsbehov = hendelse.behov(),
            ingenEndringIGruppe = hendelse.ingenEndringIGruppe
        )
    }
}