package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory

class OpprettJobbForFritakMeldepliktJobbUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val sakRepository: SakRepository,
    private val unleashGateway: UnleashGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        if (unleashGateway.isEnabled(BehandlingsflytFeature.FritakMeldeplikt)) {
            log.info("FritakMeldeplikt er slått på")
            /* TODO: optimaliser */
            for (sak in sakRepository.finnAlle()) {
                flytJobbRepository.leggTil(JobbInput(OpprettBehandlingFritakMeldepliktJobbUtfører).forSak(sak.id.toLong()))
            }
        } else {
            log.info("FritakMeldeplikt er slått av")
        }
    }


    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettJobbForFritakMeldepliktJobbUtfører(
                flytJobbRepository = repositoryProvider.provide(),
                sakRepository = repositoryProvider.provide(),
                unleashGateway = gatewayProvider.provide(),
            )
        }

        override val type = "batch.OpprettJobbForFritakMeldeplikt"

        override val navn = "Start jobb for å sjekke behov for revurdering pga fritak meldeplikt"

        override val beskrivelse = "Start jobb for å sjekke behov for revurdering pga fritak meldeplikt."

        override val cron = CronExpression.createWithoutSeconds("10 2 * * 3")
    }
}
