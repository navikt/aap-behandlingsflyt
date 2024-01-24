package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.SattPåVentLøsning
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.flyt.ValiderBehandlingTilstand
import no.nav.aap.behandlingsflyt.prosessering.OppgaveInput
import no.nav.aap.behandlingsflyt.prosessering.OppgaveRepository
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingOppgave
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.behandlingRepository
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Status

class BehandlingHendelseHåndterer(connection: DBConnection) {

    private val behandlingRepository = behandlingRepository(connection)
    private val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)
    private val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(connection)
    private val oppgaveRepository = OppgaveRepository(connection)
    private val kontroller = FlytOrkestrator(connection)

    fun håndtere(key: BehandlingId, hendelse: BehandlingHendelse) {
        when (hendelse) {
            is BehandlingSattPåVent -> {
                val behandling = behandlingRepository.hent(key)
                val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
                ValiderBehandlingTilstand.validerTilstandBehandling(
                    behandling = behandling,
                    eksisterenedeAvklaringsbehov = avklaringsbehovene.alle()
                )

                val kontekst = behandling.flytKontekst()

                kontroller.settBehandlingPåVent(kontekst)
            }

            else -> {
                val behandling = behandlingRepository.hent(key)
                val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
                ValiderBehandlingTilstand.validerTilstandBehandling(
                    behandling = behandling,
                    eksisterenedeAvklaringsbehov = avklaringsbehovene.alle()
                )

                val kontekst = behandling.flytKontekst()
                if (behandling.status() == Status.PÅ_VENT) {
                    val avklaringsbehovKontroller = avklaringsbehovOrkestrator
                    avklaringsbehovKontroller.løsAvklaringsbehov(
                        kontekst = kontekst,
                        avklaringsbehovene = avklaringsbehovene,
                        avklaringsbehov = SattPåVentLøsning()
                    )
                }
                oppgaveRepository.leggTil(
                    OppgaveInput(oppgave = ProsesserBehandlingOppgave).forBehandling(
                        kontekst.sakId,
                        kontekst.behandlingId
                    )
                )
            }
        }

    }
}