package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.forretningsflyt.gjenopptak.GjenopptakRepository
import no.nav.aap.motor.CronExpression
import no.nav.aap.motor.FlytOppgaveRepository
import no.nav.aap.motor.Oppgave
import no.nav.aap.motor.OppgaveInput
import no.nav.aap.motor.OppgaveUtfører

class GjenopptaBehandlingOppgaveUtfører(
    private val gjenopptakRepository: GjenopptakRepository,
    private val oppgaveRepository: FlytOppgaveRepository
) : OppgaveUtfører {

    override fun utfør(input: OppgaveInput) {
        val behandlingerForGjennopptak = gjenopptakRepository.finnBehandlingerForGjennopptak()

        behandlingerForGjennopptak.forEach { sakOgBehandling ->
            val oppgaverPåBehandling = oppgaveRepository.hentOppgaveForBehandling(sakOgBehandling.behandlingId)

            if (oppgaverPåBehandling.none { it.oppgave.type() == ProsesserBehandlingOppgaveUtfører.type() }) {
                oppgaveRepository.leggTil(
                    OppgaveInput(ProsesserBehandlingOppgaveUtfører).forBehandling(
                        sakId = sakOgBehandling.sakId,
                        behandlingId = sakOgBehandling.behandlingId
                    )
                )
            }
        }
    }

    companion object : Oppgave {
        override fun konstruer(connection: DBConnection): OppgaveUtfører {
            return GjenopptaBehandlingOppgaveUtfører(
                GjenopptakRepository(connection),
                FlytOppgaveRepository(connection)
            )
        }

        override fun type(): String {
            return "batch.gjenopptaBehandlinger"
        }

        override fun cron(): CronExpression {
            return CronExpression.create("0 0 7 * * *")
        }
    }
}
