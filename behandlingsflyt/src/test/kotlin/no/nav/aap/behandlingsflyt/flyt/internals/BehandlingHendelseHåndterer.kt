package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository

class BehandlingHendelseHåndterer(connection: DBConnection) {

    private val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(
        connection,
        BehandlingHendelseServiceImpl(
            FlytJobbRepository((connection)),
            BrevbestillingRepositoryImpl((connection)),
            SakService(SakRepositoryImpl(connection))
        )
    )

    fun håndtere(key: BehandlingId, hendelse: BehandlingHendelse) {
        avklaringsbehovOrkestrator.taAvVentHvisPåVentOgFortsettProsessering(key)
    }
}