package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory

class OpprettBehandlingMigrereRettighetsperiodeJobbUtfører(
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val sakRepository: SakRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val underveisRepository: UnderveisRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val unleashGateway: UnleashGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("team-logs")

    override fun utfør(input: JobbInput) {

        val sakId = input.sakId()
        val sak = sakRepository.hent(SakId(sakId))
        log.info("Migrerer rettighetsperiode for sak $sakId")


        if (unleashGateway.isEnabled(BehandlingsflytFeature.MigrerRettighetsperiode)) {
            val behandlingFørMigrering = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sak.id)
                ?: error("Fant ikke behandling for sak=${sakId}")
            sakOgBehandlingService.overstyrRettighetsperioden(sak.id, sak.rettighetsperiode.fom, Tid.MAKS)
            val utvidVedtakslengdeBehandling = opprettNyBehandling(sak)
            prosesserBehandlingService.triggProsesserBehandling(utvidVedtakslengdeBehandling)
            val behandlingEtterMigrering = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sak.id)
                ?: error("Fant ikke behandling for sak=${sakId}")
            validerBehandlingerErUlike(behandlingFørMigrering, behandlingEtterMigrering)
            validerRettighetstype(behandlingFørMigrering, behandlingEtterMigrering)
            validerTilkjentYtelse(behandlingFørMigrering, behandlingEtterMigrering)
            validerUnderveisPerioder(behandlingFørMigrering, behandlingEtterMigrering)

            log.info("Jobb for migrering av rettighetsperiode fullført for sak ${sakId}")
        } else {
            log.info("Featuretoggle er skrudd av - migrerer ikke")
        }
    }

    private fun validerRettighetstype(
        behandlingFørMigrering: Behandling,
        behandlingEtterMigrering: Behandling
    ) {
        val vilkårFør = vilkårsresultatRepository.hent(behandlingFørMigrering.id)
        val vilkårEtter = vilkårsresultatRepository.hent(behandlingEtterMigrering.id)
        val rettighetstypeFør = vilkårFør.rettighetstypeTidslinje()
        val rettighetstypeEtter = vilkårEtter.rettighetstypeTidslinje()
        if (rettighetstypeFør.isEmpty() && rettighetstypeEtter.isEmpty()) {
            log.info("Rettighetstypen er tom før og etter migrering - totalt avslag")
            return
        }
        val rettighetstypeEtterBegrenset = rettighetstypeEtter.begrensetTil(rettighetstypeFør.helePerioden())
        secureLogger.info("Migrering vilkår før=${rettighetstypeFør} og etter=$rettighetstypeEtter")
        if (rettighetstypeEtterBegrenset != rettighetstypeFør) {
            secureLogger.info("Migrering vilkår før=${rettighetstypeFør} og etter=$rettighetstypeEtterBegrenset")
            throw IllegalStateException("Vilkår før og etter migrering er ulik i den ")
        }
    }

    private fun validerUnderveisPerioder(
        behandlingFørMigrering: Behandling,
        behandlingEtterMigrering: Behandling
    ) {
        val underveisFør = underveisRepository.hentHvisEksisterer(behandlingFørMigrering.id)?.somTidslinje()?.komprimer()?.segmenter()?.toList()
            ?: error("Fant ikke underveis for behandling ${behandlingFørMigrering.id}")
        val underveisEtter = underveisRepository.hentHvisEksisterer(behandlingEtterMigrering.id)?.somTidslinje()?.komprimer()?.segmenter()?.toList()
            ?: error("Fant ikke underveis for behandling ${behandlingEtterMigrering.id}")
        secureLogger.info("Migrering underveis før=$underveisFør og etter=$underveisEtter")
        if (underveisFør.size != underveisEtter.size) {
            secureLogger.info("Migrering underveis før=$underveisFør og etter=$underveisEtter")
            throw IllegalStateException("Ulikt antall underveisperioder før ${underveisFør.size} og etter migrering ${underveisEtter.size}")
        }
        underveisFør.forEachIndexed { index, periodeFør ->
            val periodeEtter = underveisEtter.find { it.periode == periodeFør.periode }
                ?: error("Fant ikke underveisperiode for ny behandling for indeks: ${index}")
            if (periodeFør != periodeEtter) {
                secureLogger.info("Migrering underveis før=$periodeFør og etter=$periodeEtter")
                throw IllegalStateException("Ulike underveisperioder mellom ny og gammel behandling for indeks: ${index}")
            }
        }
    }

    private fun validerTilkjentYtelse(
        behandlingFørMigrering: Behandling,
        behandlingEtterMigrering: Behandling
    ) {
        val tilkjentYtelseFør = tilkjentYtelseRepository.hentHvisEksisterer(behandlingFørMigrering.id)?.tilTidslinje()?.komprimer()?.segmenter()?.toList()
            ?: emptyList()
        val tilkjentYtelseEtter = tilkjentYtelseRepository.hentHvisEksisterer(behandlingEtterMigrering.id)?.tilTidslinje()?.komprimer()?.segmenter()?.toList()
            ?: emptyList()
        secureLogger.info("Migrering tilkjent ytelse før=$tilkjentYtelseFør og etter=$tilkjentYtelseEtter")
        if (tilkjentYtelseEtter.size != tilkjentYtelseFør.size) {
            throw IllegalStateException("Ulikt antall tilkjent ytelseperioder mellom ny ${tilkjentYtelseEtter.size} og gammel behandling ${tilkjentYtelseFør.size}")
        }
        tilkjentYtelseFør.forEachIndexed { index, periodeFør ->
            val periodeEtter = tilkjentYtelseEtter.find { it.periode == periodeFør.periode }
            if (periodeEtter == null) {
                throw IllegalStateException("Mangler periode ${periodeFør} med tilkjent ytelse i ny behandling - indeks: $index")
            } else if (periodeEtter != periodeFør) {
                secureLogger.info("Migrering tilkjent ytelse før=$periodeFør og etter=$periodeEtter")
                throw IllegalStateException("Ulike perioder i tilkjent ytelse mellom ny og gammel behandling - indeks: $index")
            }
        }
    }

    private fun validerBehandlingerErUlike(
        behandlingFørMigrering: Behandling,
        behandlingEtterMigrering: Behandling,
    ) {
        if (behandlingEtterMigrering.id == `behandlingFørMigrering`.id) {
            throw IllegalStateException("Skal ha ulik behandling før og etter migrering av rettighetsperiode")
        }
    }

    private fun opprettNyBehandling(sak: Sak): SakOgBehandlingService.OpprettetBehandling =
        sakOgBehandlingService.finnEllerOpprettBehandling(
            sakId = sak.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                årsak = ÅrsakTilOpprettelse.MIGRER_RETTIGHETSPERIODE,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.MIGRER_RETTIGHETSPERIODE))
            ),
        )

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettBehandlingMigrereRettighetsperiodeJobbUtfører(
                prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                sakRepository = repositoryProvider.provide(),
                underveisRepository = repositoryProvider.provide(),
                tilkjentYtelseRepository = repositoryProvider.provide(),
                vilkårsresultatRepository = repositoryProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
                unleashGateway = gatewayProvider.provide(),
            )
        }

        override val type = "batch.MigrerRettighetsperiodeJobbUtfører"

        override val navn = "Migrere rettighetsperiode for sak med begrenset varighet på rettighetsperioden"

        override val beskrivelse =
            "Starter ny behandling som endrer rettighetsperioden og løper igjennom vilkår og beregning"
    }
}