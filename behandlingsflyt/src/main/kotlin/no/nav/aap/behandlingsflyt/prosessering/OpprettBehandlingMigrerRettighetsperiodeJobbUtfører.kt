package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory

class OpprettBehandlingMigrerRettighetsperiodeJobbUtfører(
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val unleashGateway: UnleashGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    fun harKunAvsluttedeBehandlinger(sak: Sak): Boolean {
        val behandlinger = behandlingRepository.hentAlleFor(sak.id, listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering))
        return behandlinger.all { it.status().erAvsluttet() }
    }

    override fun utfør(input: JobbInput) {
        val sakerForMigrering = finnSakerSomSkalMigreres()
        log.info("Fant ${sakerForMigrering.size} som kun har avsluttede behandlinger og kan migrere rettighetsperiode")
        if (unleashGateway.isEnabled(BehandlingsflytFeature.MigrerRettighetsperiode)) {
            sakerForMigrering.forEach { sak ->
                val fritakMeldepliktBehandling = opprettNyBehandling(sak)
                sakRepository.oppdaterRettighetsperiode(sak.id, Periode(sak.rettighetsperiode.fom, Tid.MAKS))
                prosesserBehandlingService.triggProsesserBehandling(fritakMeldepliktBehandling)
                log.info("Oppretter behandling for migrering av rettighetsperiode for saksnummer ${sak.saksnummer}")
            }
        } else {
            log.info("Toggle MigrerRettighetsperiode er skrudd av og kjører ikke")
        }
    }

    private fun opprettNyBehandling(sak: Sak): SakOgBehandlingService.OpprettetBehandling =
        sakOgBehandlingService.finnEllerOpprettBehandling(
            sakId = sak.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                årsak = ÅrsakTilOpprettelse.AUTOMATISK_OPPDATER_VILKÅR,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.AUTOMATISK_OPPDATER_VILKÅR))
            ),
        )

    private fun finnSakerSomSkalMigreres(): List<Sak> {
        val sakerForMigrering = sakRepository.finnSakerMedUtenRiktigSluttdatoPåRettighetsperiode()
        log.info("Fant ${sakerForMigrering.size} som har feil rettighetsperiode")
        return sakerForMigrering.filter { harKunAvsluttedeBehandlinger(it) }
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettBehandlingMigrerRettighetsperiodeJobbUtfører(
                prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                sakRepository = repositoryProvider.provide(),
                behandlingRepository = repositoryProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
                unleashGateway = gatewayProvider.provide(),
            )
        }

        override val type = "batch.MigrerRettighetsperiodeJobbUtfører"

        override val navn = "Utvid rettighetsperiode til Tid.Maks for gamle saker"

        override val beskrivelse = "Skal endre rettighetsperiodens sluttdato til Tid.Maks for saker hvor denne ble satt til 1 år. For saker som har en åpen behandling må man avvente dette"

        /**
         * Kjøres hver dag kl 05:00
         */
        override val cron = CronExpression.createWithoutSeconds("0 5 * * *")
    }
}
