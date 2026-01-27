package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
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

class OpprettJobbForMigrereRettighetsperiodeJobbUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val sakRepository: SakRepository,
    private val trukketSøknadService: TrukketSøknadService,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val unleashGateway: UnleashGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {

        val saker = sakRepository.finnSakerMedUtenRiktigSluttdatoPåRettighetsperiode()
        val sakerForMigrering = saker.filter { sak ->
            val sisteYtelsesbehandling = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sak.id)
            if (sisteYtelsesbehandling != null) {
                val erTrukket = trukketSøknadService.søknadErTrukket(sisteYtelsesbehandling.id)
                sisteYtelsesbehandling.status().erAvsluttet() && !erTrukket
            } else {
                log.info("Fant ikke ytelsesbehandlinger for sak ${sak.id} ")
                false
            }
        }.filter { erForhåndskvalifisertSak(it) }

        log.info("Fant ${saker.size} migrering av rettighetsperiode. Antall iverksatte/avsluttede kandidater: ${sakerForMigrering.size}")

        if (unleashGateway.isEnabled(BehandlingsflytFeature.MigrerRettighetsperiode)) {
            sakerForMigrering.forEach { sak ->
                flytJobbRepository.leggTil(JobbInput(OpprettBehandlingMigrereRettighetsperiodeJobbUtfører).forSak(sak.id.toLong()))
            }

            log.info("Jobb for migrering av rettighetsperiode fullført for ${sakerForMigrering.size}")
        }
    }

    /**
     * Før vi skrur på for fullt ønsker vi å teste enkeltsaker i hvert miljø
     */
    fun erForhåndskvalifisertSak(sak: Sak): Boolean {
        val forhåndskvalifisertDev = listOf<String>()
        val forhåndskvalifisertProd = listOf<String>(
                    "4LFDNAo",
                    "4LFY0BK",
                    "4LG2AN4",
                    "4LG8Q4G",
                    "4LGiDCG",
                    "4LGPVEo",
                    "4LGQXZK",
                    "4LGS0KG",
                    "4LGT35C",
                    "4LGWAW0",

        )
        return when (Miljø.er()) {
            MiljøKode.DEV -> forhåndskvalifisertDev.contains(sak.saksnummer.toString())
            MiljøKode.PROD -> forhåndskvalifisertProd.contains(sak.saksnummer.toString())
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