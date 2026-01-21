package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
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

class OpprettJobbForMigrereRettighetsperiodeJobbUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val sakRepository: SakRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val unleashGateway: UnleashGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {

        val saker = sakRepository.finnSakerMedUtenRiktigSluttdatoPåRettighetsperiode()
        val sakerUtenÅpenBehandling = saker.filter { sak ->
                sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sak.id)?.status()?.erAvsluttet()
                ?: error("Fant ikke behandling for sak=${sak.id}")
        }
        log.info("Fant ${saker.size} migrering av rettighetsperiode. Antall iverksatte/avsluttede kandidater: ${sakerUtenÅpenBehandling.size}")

        if (unleashGateway.isEnabled(BehandlingsflytFeature.MigrerRettighetsperiode)) {
            sakerUtenÅpenBehandling.forEach { sak ->
                flytJobbRepository.leggTil(JobbInput(OpprettBehandlingMigrereRettighetsperiodeJobbUtfører).forSak(sak.id.toLong()))
            }

            log.info("Jobb for migrering av rettighetsperiode fullført for ${sakerUtenÅpenBehandling.size}")
        }
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettJobbForMigrereRettighetsperiodeJobbUtfører(
                flytJobbRepository = repositoryProvider.provide(),
                sakRepository = repositoryProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
                unleashGateway = gatewayProvider.provide(),
            )
        }

        override val type = "batch.OpprettJobbForMigrerRettighetsperioder"

        override val navn = "Oppretter jobber for de sakene som skal migrere rettighetsperiode til Tid.MAKS"

        override val beskrivelse =
            "Skal trigge ny jobb for hver sak som trenger å migrere rettighetsperiode"

        /**
         * Kjøres hver dag kl 04:00
         */
        override val cron = CronExpression.createWithoutSeconds("0 4 * * *")
    }
}