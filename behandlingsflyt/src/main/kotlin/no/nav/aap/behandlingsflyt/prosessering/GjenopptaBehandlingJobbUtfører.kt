package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.forretningsflyt.gjenopptak.GjenopptakRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProviderJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression

class GjenopptaBehandlingJobbUtfører(
    private val gjenopptakRepository: GjenopptakRepository,
    private val flytJobbRepository: FlytJobbRepository
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val behandlingerForGjennopptak = gjenopptakRepository.finnBehandlingerForGjennopptak()

        behandlingerForGjennopptak.forEach { sakOgBehandling ->
            val jobberPåBehandling = flytJobbRepository.hentJobberForBehandling(sakOgBehandling.behandlingId.toLong())

            if (jobberPåBehandling.none { it.type() == ProsesserBehandlingJobbUtfører.type }) {
                flytJobbRepository.leggTil(
                    JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(
                        sakID = sakOgBehandling.sakId.toLong(),
                        behandlingId = sakOgBehandling.behandlingId.toLong()
                    )
                )
            }
        }
    }

    companion object : ProviderJobbSpesifikasjon {
        override val type = "batch.gjenopptaBehandlinger"
        override val navn = "Gjenoppta behandling"
        override val beskrivelse = "Finner behandlinger som er satt på vent og fristen har løpt ut. Gjenopptar behandlingen av disse slik at saksbehandler kan fortsette på saksbehandling av saken"
        override val cron = CronExpression.create("0 0 7 * * *")

        override fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører {
            return GjenopptaBehandlingJobbUtfører(
                gjenopptakRepository = repositoryProvider.provide(),
                flytJobbRepository = repositoryProvider.provide(),
            )
        }
    }
}
