package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.motor.Oppgave
import no.nav.aap.motor.OppgaveInput
import no.nav.aap.motor.OppgaveUtfører

class StoppetHendelseOppgaveUtfører(
    private val behandlingHendelseService: BehandlingHendelseService,
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : OppgaveUtfører {

    override fun utfør(input: OppgaveInput) {
        val behandlingId = input.behandlingId()
        val behandling = behandlingRepository.hent(behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)

        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
    }

    companion object : Oppgave {
        override fun konstruer(connection: DBConnection): OppgaveUtfører {
            return StoppetHendelseOppgaveUtfører(
                BehandlingHendelseService(SakService(connection)),
                BehandlingRepositoryImpl(connection),
                AvklaringsbehovRepositoryImpl(connection)
            )
        }

        override fun type(): String {
            return "flyt.hendelse"
        }
    }
}
