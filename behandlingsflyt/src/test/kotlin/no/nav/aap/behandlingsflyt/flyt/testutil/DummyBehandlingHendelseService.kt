package no.nav.aap.behandlingsflyt.flyt.testutil

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling

object DummyBehandlingHendelseService : BehandlingHendelseService {
    override fun stoppet(
        behandling: Behandling,
        avklaringsbehovene: Avklaringsbehovene
    ) {

    }
    override fun stoppetMedReservasjon(
        behandling: Behandling,
        avklaringsbehovene: Avklaringsbehovene,
        reserverTil: String?
    ) {

    }
}