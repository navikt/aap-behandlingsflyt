package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
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
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate

class OpprettBehandlingMigrereRettighetsperiodeJobbUtfører(
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val sakRepository: SakRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val underveisRepository: UnderveisRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("team-logs")

    override fun utfør(input: JobbInput) {

        val sakId = input.sakId()
        val sak = sakRepository.hent(SakId(sakId))
        log.info("Migrerer rettighetsperiode for sak $sakId")


        if (sak.rettighetsperiode.tom == Tid.MAKS) {
            log.info("Har allerede tid maks som rettighetsperiode - lager ikke en ny behandling")
            return
        }
        val behandlingFørMigrering = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sak.id)
            ?: error("Fant ikke behandling for sak=${sakId}")
        if (behandlingFørMigrering.status().erÅpen()) {
            throw IllegalArgumentException("Kan ikke migrere sak når det finnes en åpen behandling")
        }
        sakOgBehandlingService.overstyrRettighetsperioden(sak.id, sak.rettighetsperiode.fom, Tid.MAKS)
        val utvidVedtakslengdeBehandling = opprettNyBehandling(sak)
        prosesserBehandlingService.triggProsesserBehandling(utvidVedtakslengdeBehandling)
        validerTilstandEtterMigrering(sak, sakId, behandlingFørMigrering)

        log.info("Jobb for migrering av rettighetsperiode fullført for sak ${sakId}")

    }

    private fun validerTilstandEtterMigrering(
        sak: Sak,
        sakId: Long,
        behandlingFørMigrering: Behandling
    ) {
        // Kan ikke validere alle sakene i dev ettersom ekstremt mange er i en ugyldig tilstand
        if (Miljø.erDev()) {
            return
        }
        val behandlingEtterMigrering = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sak.id)
            ?: error("Fant ikke behandling for sak=${sakId}")
        validerBehandlingerErUlike(behandlingFørMigrering, behandlingEtterMigrering)
        validerRettighetstype(behandlingFørMigrering, behandlingEtterMigrering, sak)
        validerTilkjentYtelse(behandlingFørMigrering, behandlingEtterMigrering, sak)
        validerUnderveisPerioder(behandlingFørMigrering, behandlingEtterMigrering, sak)
    }

    private fun validerRettighetstype(
        behandlingFørMigrering: Behandling,
        behandlingEtterMigrering: Behandling,
        sak: Sak
    ) {
        val vilkårFør = vilkårsresultatRepository.hent(behandlingFørMigrering.id)
        val vilkårEtter = vilkårsresultatRepository.hent(behandlingEtterMigrering.id)
        val rettighetstypeFør = vilkårFør.rettighetstypeTidslinje()
        val rettighetstypeEtter = vilkårEtter.rettighetstypeTidslinje()
        if (rettighetstypeFør.isEmpty() && rettighetstypeEtter.isEmpty()) {
            log.info("Rettighetstypen er tom før og etter migrering - totalt avslag")
            return
        } else if (rettighetstypeFør.isEmpty()) {
            log.warn("Rettighetstypen er tom før migrering, men finnes etter migrering - bør følges opp")
            return
        }
        val rettighetstypeEtterBegrenset = rettighetstypeEtter.begrensetTil(rettighetstypeFør.helePerioden())
        secureLogger.info("Migrering vilkår før=${rettighetstypeFør} og etter=$rettighetstypeEtter")
        if(skalIgnorereTilkjentYtelseSjekk(sak)) {
            return
        }
        if (rettighetstypeEtterBegrenset != rettighetstypeFør) {
            secureLogger.info("Migrering vilkår før=${rettighetstypeFør} og etter=$rettighetstypeEtterBegrenset")
            secureLogger.info("Vilkår før=$vilkårFør og etter=$vilkårEtter")
            throw IllegalStateException("Vilkår før og etter migrering er ulik i den ")
        }
    }

    private fun validerUnderveisPerioder(
        behandlingFørMigrering: Behandling,
        behandlingEtterMigrering: Behandling,
        sak: Sak
    ) {
        /**
         * Må nulle ut periode og id for å kunne komprimere og se reelle forskjeller på underveisperiodene
         * Hvis forrige var førstegangsbehandling vil meldepliktstatus naturlig endre seg
         */
        val forrigeBehandlingFørstegangsbehandling = behandlingFørMigrering.typeBehandling() == TypeBehandling.Førstegangsbehandling
        fun overstyrVerdierForPeriode(underveisperiode: Underveisperiode): Underveisperiode =
            underveisperiode.copy(
                periode = Periode(Tid.MIN, Tid.MAKS),
                id = null,
                meldepliktStatus = if (forrigeBehandlingFørstegangsbehandling) null else underveisperiode.meldepliktStatus
            )

        val underveisFør =
            underveisRepository.hentHvisEksisterer(behandlingFørMigrering.id)?.somTidslinje()
                ?.map { overstyrVerdierForPeriode(it) }?.komprimer()
                ?.segmenter()?.toList()
                ?: emptyList()
        val underveisEtter =
            underveisRepository.hentHvisEksisterer(behandlingEtterMigrering.id)?.somTidslinje()
                ?.map { overstyrVerdierForPeriode(it) }?.komprimer()
                ?.segmenter()?.toList()
                ?: error("Fant ikke underveis for behandling ${behandlingEtterMigrering.id}")
        secureLogger.info("Migrering underveis før=$underveisFør og etter=$underveisEtter")

        if (underveisFør.isEmpty() && underveisEtter.isNotEmpty()) {
            if (underveisEtter.any { it.verdi.utfall == Utfall.OPPFYLT }) {
               throw IllegalStateException("Mangler underveis tidligere, men fant innvilget periode etter migreringen")
            }
        } else if (skalValidereUnderveis(sak, behandlingFørMigrering)) {
            if (underveisFør.size != underveisEtter.size) {
                log.info("Ulikt antall underveisperioder før ${underveisFør.size} og etter migrering ${underveisEtter.size}")
            }
            underveisFør.forEachIndexed { index, periodeFør ->
                val periodeEtter = underveisEtter.find { it.periode == periodeFør.periode }
                    ?: error("Fant ikke underveisperiode for ny behandling for indeks: ${index}")
                val verdiFør = periodeFør.verdi
                val verdiEtter = periodeEtter.verdi
                if (verdiFør.meldePeriode != verdiEtter.meldePeriode
                    || verdiFør.avslagsårsak != verdiEtter.avslagsårsak
                    || verdiFør.brukerAvKvoter != verdiEtter.brukerAvKvoter
                    || verdiFør.grenseverdi != verdiEtter.grenseverdi
                    || verdiFør.arbeidsgradering.opplysningerMottatt != verdiEtter.arbeidsgradering.opplysningerMottatt
                    || verdiFør.arbeidsgradering.fastsattArbeidsevne != verdiEtter.arbeidsgradering.fastsattArbeidsevne
                    || verdiFør.arbeidsgradering.totaltAntallTimer != verdiEtter.arbeidsgradering.totaltAntallTimer
                    || verdiFør.rettighetsType != verdiEtter.rettighetsType
                    || verdiFør.utfall != verdiEtter.utfall
                    || verdiFør.trekk != verdiEtter.trekk
                    || verdiFør.institusjonsoppholdReduksjon != verdiEtter.institusjonsoppholdReduksjon
                    || !likArbeidsgradering(verdiFør.arbeidsgradering, verdiEtter.arbeidsgradering)
                ) {
                    // Spesialsjekk på meldeplitstatus, gradering,
                    secureLogger.info("Migrering underveis før=$periodeFør og etter=$periodeEtter")
                    throw IllegalStateException("Ulike underveisperioder mellom ny og gammel behandling for indeks: ${index}")
                }
            }
        }
    }


    private fun skalIgnorereTilkjentYtelseSjekk (sak: Sak) : Boolean =
        listOf("4MD3UPS", "4LDZA0W").contains(sak.saksnummer.toString())
    /**
     * Kan ikke validere underveis hvis siste behandling er fastsatt periode passert - da vil de av natur bli splittet ulikt og være ulike
     */
    private fun skalValidereUnderveis(sak: Sak, behandlingFørMigrering: Behandling): Boolean {
        val erForrigeBehandlingFastsattPeriodePassert =
            behandlingFørMigrering.vurderingsbehov().map { it.type }.contains(Vurderingsbehov.FASTSATT_PERIODE_PASSERT)
        val forhåndsgodkjenteSaksnummerMedPotensiellEndringIUnderveis = listOf<String>(
            "4MD3UPS",
            "4oAoCR4",
            "4LDZA0W"
        )
        val skalIgnoreres =
            forhåndsgodkjenteSaksnummerMedPotensiellEndringIUnderveis.contains(sak.saksnummer.toString())
        return !(erForrigeBehandlingFastsattPeriodePassert || skalIgnoreres)
    }


    /**
     * På grunn av endring i logisk oppbygging av underveisperioder har vi flippet arbeidsgradering andel arbeid og gradering
     * i løpet av 2025 for å ikke innvilge fremtidige perioder
     */
    fun likArbeidsgradering(
        arbeidsgraderingFør: ArbeidsGradering,
        arbeidsgraderingEtter: ArbeidsGradering
    ): Boolean {
        if (arbeidsgraderingFør.gradering != arbeidsgraderingEtter.gradering) {
            if (arbeidsgraderingFør.gradering.komplement() != arbeidsgraderingEtter.gradering) {
                return false
            }
        }
        if (arbeidsgraderingFør.andelArbeid != arbeidsgraderingEtter.andelArbeid) {
            if (arbeidsgraderingFør.andelArbeid.komplement() != arbeidsgraderingEtter.andelArbeid) {
                return false
            }
        }

        return true
    }

    private fun validerTilkjentYtelse(
        behandlingFørMigrering: Behandling,
        behandlingEtterMigrering: Behandling,
        sak: Sak
    ) {
        val tilkjentYtelseEffektivDagsatsFør =
            tilkjentYtelseRepository.hentHvisEksisterer(behandlingFørMigrering.id)
                ?.somTidslinje({ it.periode }, {
                    // Tidligere i Kelvin ble dagsats satt til en verdi frem i tid, dette er feil og skjer ikke lenger.
                    // Da må vi sammenligne med det som BURDE vært dagsats
                    if (it.periode.fom > LocalDate.now()) Beløp(0) else it.tilkjent.redusertDagsats()
                })?.komprimer()
                ?.segmenter()?.toList()
                ?: emptyList()
        // Før fikk man satt dagsats i tilkjent ytelse frem i tid - dette bør ikke skje
        val tilkjentYtelseEffektivDagsatsEtter =
            tilkjentYtelseRepository.hentHvisEksisterer(behandlingEtterMigrering.id)
                ?.somTidslinje({ it.periode }, { it.tilkjent.redusertDagsats() })?.komprimer()
                ?.segmenter()?.toList()
                ?: emptyList()
        secureLogger.info("Migrering tilkjent ytelse før=$tilkjentYtelseEffektivDagsatsFør og etter=$tilkjentYtelseEffektivDagsatsEtter")
        if (skalIgnorereTilkjentYtelseSjekk(sak)) {
            return
        }
        if (tilkjentYtelseEffektivDagsatsEtter.size != tilkjentYtelseEffektivDagsatsFør.size) {
            /**
             * Lagret ikke ned tilkjent ytelse på rene avslag tidligere, men startet med det i november/desember 2025.
             * Nye behandlinger genererer dermed tilkjent ytelse for rene avslag.
             * Vi har også ulik oppførsel på graderinger før og etter november/desember
             */
            if (tilkjentYtelseEffektivDagsatsFør.isEmpty()) {
                if (tilkjentYtelseEffektivDagsatsEtter.any { it.verdi.verdi() > BigDecimal.ZERO }) {
                    throw IllegalStateException("Har gått fra totalt avslag til å få tilkjent ytelse med mulig utbetaling siden redusert dagsats ikke er 0")
                }
            } else if (Miljø.erProd()) {
                throw IllegalStateException("Ulikt antall tilkjent ytelseperioder mellom ny ${tilkjentYtelseEffektivDagsatsEtter.size} og gammel behandling ${tilkjentYtelseEffektivDagsatsFør.size}")
            } else {
                log.warn("Ulikt antall tilkjent ytelseperioder ved migrering - godtas i dev pga gammel data")
            }
        }
        tilkjentYtelseEffektivDagsatsFør.forEachIndexed { index, periodeFør ->
            val periodeEtter = tilkjentYtelseEffektivDagsatsEtter.find { it.periode == periodeFør.periode }
            if (periodeEtter == null) {
                throw IllegalStateException("Mangler periode ${periodeFør} med tilkjent ytelse i ny behandling - indeks: $index")
            } else if (periodeEtter != periodeFør) {
                secureLogger.info("Migrering tilkjent ytelse før=$periodeFør og etter=$periodeEtter")
                log.warn("Ulik tilkjent ytelseperiode ved migrering - godtas i dev pga gammel data")
                if (Miljø.erProd()) {
                    throw IllegalStateException("Ulike perioder i tilkjent ytelse mellom ny og gammel behandling - indeks: $index")
                }
            }
        }
    }

    private fun validerBehandlingerErUlike(
        behandlingFørMigrering: Behandling,
        behandlingEtterMigrering: Behandling,
    ) {
        if (behandlingEtterMigrering.id == behandlingFørMigrering.id) {
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
            )
        }

        override val type = "batch.MigrerRettighetsperiodeJobbUtfører"

        override val navn = "Migrere rettighetsperiode for sak med begrenset varighet på rettighetsperioden"

        override val beskrivelse =
            "Starter ny behandling som endrer rettighetsperioden og løper igjennom vilkår og beregning"
    }
}