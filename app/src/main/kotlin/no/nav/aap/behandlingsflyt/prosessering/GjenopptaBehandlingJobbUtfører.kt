package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.forretningsflyt.gjenopptak.GjenopptakRepository
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression

class GjenopptaBehandlingJobbUtfører(
    private val gjenopptakRepository: GjenopptakRepository,
    private val flytJobbRepository: FlytJobbRepository
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val behandlingerForGjennopptak = gjenopptakRepository.finnBehandlingerForGjennopptak()

        behandlingerForGjennopptak.forEach { sakOgBehandling ->
            val jobberPåBehandling = flytJobbRepository.hentOppgaveForBehandling(sakOgBehandling.behandlingId)

            if (jobberPåBehandling.none { it.jobb.type() == ProsesserBehandlingJobbUtfører.type() }) {
                flytJobbRepository.leggTil(
                    JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(
                        sakId = sakOgBehandling.sakId,
                        behandlingId = sakOgBehandling.behandlingId
                    )
                )
            }
        }
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return GjenopptaBehandlingJobbUtfører(
                GjenopptakRepository(connection),
                FlytJobbRepository(connection)
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
