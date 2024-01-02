package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.flyt.ValiderBehandlingTilstand
import no.nav.aap.behandlingsflyt.flyt.tilKontekst

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

        val kontekst = tilKontekst(behandling)

        avklaringsbehovOrkestrator.løsAvklaringsbehovOgFortsettProsessering(
            kontekst = kontekst,
            avklaringsbehov = hendelse.behov(),
            ingenEndringIGruppe = hendelse.ingenEndringIGruppe
        )
    }
}