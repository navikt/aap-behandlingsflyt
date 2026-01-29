package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory
import java.time.LocalDate

class OpprettJobbForMigrereRettighetsperiodeJobbUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val sakRepository: SakRepository,
    private val trukketSøknadService: TrukketSøknadService,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val unleashGateway: UnleashGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)
    private val førsteJanuar2026 = LocalDate.of(2026, 1, 1).atStartOfDay()

    override fun utfør(input: JobbInput) {

        val saker = sakRepository.finnSakerMedUtenRiktigSluttdatoPåRettighetsperiode()
        val sakerForMigrering = saker
            .filter { sak ->
                val sisteYtelsesbehandling = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sak.id)
                if (sisteYtelsesbehandling != null) {
                    erAktuellForMigrering(sisteYtelsesbehandling)
                } else {
                    log.info("Fant ikke ytelsesbehandlinger for sak ${sak.id} ")
                    false
                }

            }
            .filter { erForhåndskvalifisertSak(it) }
            .take(25)

        log.info("Fant ${saker.size} migrering av rettighetsperiode. Antall iverksatte/avsluttede kandidater: ${sakerForMigrering.size}")

        if (unleashGateway.isEnabled(BehandlingsflytFeature.MigrerRettighetsperiode)) {
            sakerForMigrering.forEach { sak ->
                flytJobbRepository.leggTil(JobbInput(OpprettBehandlingMigrereRettighetsperiodeJobbUtfører).forSak(sak.id.toLong()))
            }

            log.info("Jobb for migrering av rettighetsperiode fullført for ${sakerForMigrering.size}")
        }
    }

    /**
     * Kan kun behandle de som er avsluttet og ønsker ikke å migrere de som er trukket
     * For første gjennomkjøring ønsker vi kun å behandle de sakene hvor det finnes en
     * behandling ila 2026 - disse har størst sjanse for å ikke ha diff i tilkjent ytelse, underveis eller vilkår
     */
    private fun erAktuellForMigrering(sisteYtelsesbehandling: Behandling): Boolean =
        sisteYtelsesbehandling.status().erAvsluttet()
                && !harSøknadTrukket(sisteYtelsesbehandling)
                && erOpprettetI2026(sisteYtelsesbehandling)

    private fun erOpprettetI2026(sisteYtelsesbehandling: Behandling): Boolean =
        sisteYtelsesbehandling.opprettetTidspunkt.isAfter(førsteJanuar2026)

    private fun harSøknadTrukket(sisteYtelsesbehandling: Behandling): Boolean =
        trukketSøknadService.søknadErTrukket(sisteYtelsesbehandling.id)

    /**
     * Før vi skrur på for fullt ønsker vi å teste enkeltsaker i hvert miljø
     */
    fun erForhåndskvalifisertSak(sak: Sak): Boolean {
        val forhåndskvalifisertDev = listOf<String>()
        return when (Miljø.er()) {
            MiljøKode.DEV -> false
            MiljøKode.PROD -> true // forhåndskvalifisertProd.contains(sak.saksnummer.toString())
            MiljøKode.LOKALT -> true
        }
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettJobbForMigrereRettighetsperiodeJobbUtfører(
                flytJobbRepository = repositoryProvider.provide(),
                sakRepository = repositoryProvider.provide(),
                trukketSøknadService = TrukketSøknadService(repositoryProvider),
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