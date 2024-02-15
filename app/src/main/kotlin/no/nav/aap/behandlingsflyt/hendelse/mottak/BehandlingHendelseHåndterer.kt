package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BehandlingHendelseHåndterer(connection: DBConnection) {

    private val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(connection)

    fun håndtere(key: BehandlingId, hendelse: BehandlingHendelse) {
        when (hendelse) {
            is BehandlingSattPåVent -> {
                avklaringsbehovOrkestrator.settBehandlingPåVent(key)
            }

            else -> {
                avklaringsbehovOrkestrator.løsAvklaringsbehovOgFortsettProsessering(key)
            }
        }
    }
}