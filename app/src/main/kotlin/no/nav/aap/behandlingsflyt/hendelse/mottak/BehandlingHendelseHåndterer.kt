package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BehandlingHendelseHåndterer(connection: DBConnection) {

    private val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(
        connection,
        BehandlingHendelseService(FlytJobbRepository((connection)), SakService((connection)))
    )

    fun håndtere(key: BehandlingId, hendelse: BehandlingHendelse) {
        when (hendelse) {
            is BehandlingSattPåVent -> {
                avklaringsbehovOrkestrator.settBehandlingPåVent(key, hendelse)
            }

            else -> {
                avklaringsbehovOrkestrator.taAvVentHvisPåVentOgFortsettProsessering(key)
            }
        }
    }
}