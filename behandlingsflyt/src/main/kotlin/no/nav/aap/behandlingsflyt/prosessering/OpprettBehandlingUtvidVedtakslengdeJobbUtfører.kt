package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Avslag
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetRegel
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
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
import no.nav.aap.komponenter.miljo.Miljø.erDev
import no.nav.aap.komponenter.miljo.Miljø.erLokal
import no.nav.aap.komponenter.miljo.Miljø.erProd
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDate.now

class OpprettBehandlingUtvidVedtakslengdeJobbUtfører(
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val sakRepository: SakRepository,
    private val underveisRepository: UnderveisRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val unleashGateway: UnleashGateway,
    private val clock: Clock = Clock.systemDefaultZone()
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val dryRun = false

        // Forlenger behandlinger når det er 28 dager igjen til sluttdato
        val datoHvorSakerSjekkesForUtvidelse = now().plusDays(28)

        val saker = hentKandidaterForUtvidelseAvVedtakslengde(datoHvorSakerSjekkesForUtvidelse)
            // Midlertidig sjekk for å unngå at denne jobben kjøres for åpne behandlinger
            .filter { kunSakerUtenÅpneYtelsesbehandlinger(it) }

        log.info("Fant ${saker.size} kandidater for utvidelse av vedtakslengde per $datoHvorSakerSjekkesForUtvidelse (dryRun=$dryRun)")

        if (unleashGateway.isEnabled(BehandlingsflytFeature.UtvidVedtakslengdeJobb)) {
            val resultat = saker
                .filter { if (erDev()) it.id == 4243L else if (erProd()) it.id == 1100L else if (erLokal()) true else false}
                .map { sakId ->
                    val sisteGjeldendeBehandling = sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId)
                    if (sisteGjeldendeBehandling != null) {
                        val sak = sakRepository.hent(sakId)
                        log.info("Gjeldende behandling for sak $sakId (${sak.saksnummer}) er ${sisteGjeldendeBehandling.id}")

                        // Trigger behandling som utvider vedtakslengde dersom nødvendig
                        val underveisGrunnlag = underveisRepository.hentHvisEksisterer(sisteGjeldendeBehandling.id)
                        if (underveisGrunnlag != null && (harBehovForUtvidetVedtakslengde(sisteGjeldendeBehandling.id, sakId, underveisGrunnlag, datoHvorSakerSjekkesForUtvidelse) || sak.rettighetsperiode.tom != Tid.MAKS)) {

                            // Utvider rettighetsperiode til Tid.MAKS dersom denne har en annen verdi - tom her skal
                            // på sikt fases ut og denne koden kan fjernes når alle saker har Tid.MAKS som tom
                            if (sak.rettighetsperiode.tom != Tid.MAKS) {
                                log.info("Utvider rettighetsperiode fra ${sak.rettighetsperiode.tom} til ${Tid.MAKS} for sak ${sakId}")
                                if (!dryRun) sakRepository.oppdaterRettighetsperiode(sak.id, Periode(sak.rettighetsperiode.fom, Tid.MAKS))
                            }

                            log.info("Oppretter behandling for utvidelse av vedtakslengde sak $sakId")
                            if (!dryRun) {
                                val utvidVedtakslengdeBehandling = opprettNyBehandling(sak)
                                prosesserBehandlingService.triggProsesserBehandling(utvidVedtakslengdeBehandling)
                            }
                            true

                        } else {
                            log.info("Sak med id $sakId trenger ikke utvidelse av vedtakslengde, hopper over")
                            false
                        }

                    } else {
                        log.info("Sak med id $sakId har ingen gjeldende behandlinger, hopper over")
                        false
                    }
                }

            log.info("Jobb for utvidelse av vedtakslengde fullført for ${resultat.count { it }} av ${saker.size} saker (dryRun=$dryRun)")
        }
    }

    private fun kunSakerUtenÅpneYtelsesbehandlinger(id: SakId): Boolean {
        val sisteBehandling = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(id)
        return sisteBehandling?.status() in setOf(Status.AVSLUTTET, Status.IVERKSETTES)
    }

    private fun harBehovForUtvidetVedtakslengde(
        behandlingId: BehandlingId,
        sakId: SakId,
        underveisGrunnlag: UnderveisGrunnlag,
        datoForUtvidelse: LocalDate
    ): Boolean {
        val sisteVedtatteUnderveisperiode = underveisGrunnlag.perioder.maxByOrNull { it.periode.tom }
        val rettighetstypeTidslinje = vilkårsresultatRepository.hent(behandlingId).rettighetstypeTidslinje()

        if (sisteVedtatteUnderveisperiode != null) {
            val harFremtidigRettBistandsbehov = skalUtvide(
                forrigeSluttdato = sisteVedtatteUnderveisperiode.periode.tom,
                rettighetstypeTidslinjeForInneværendeBehandling = rettighetstypeTidslinje
            )

            val gjeldendeSluttdato = sisteVedtatteUnderveisperiode.periode.tom

            log.info("Sak $sakId har harFremtidigRettBistandsbehov=$harFremtidigRettBistandsbehov og gjeldendeSluttdato=$gjeldendeSluttdato")
            return gjeldendeSluttdato.isBefore(datoForUtvidelse) && harFremtidigRettBistandsbehov
        }
        log.info("Sak $sakId har ingen vedtatte underveisperioder")
        return false
    }

    private fun hentKandidaterForUtvidelseAvVedtakslengde(dato: LocalDate): Set<SakId> {
        // TODO: Må filtrere vekk de som allerede har blitt kjørt, men ikke kvalifiserte til reell utvidelse av vedtakslengde
        return underveisRepository.hentSakerMedSisteUnderveisperiodeFørDato(dato)
    }

    fun skalUtvide(
        forrigeSluttdato: LocalDate,
        rettighetstypeTidslinjeForInneværendeBehandling: Tidslinje<RettighetsType>
    ): Boolean {
        return harFremtidigRettOrdinær(forrigeSluttdato, rettighetstypeTidslinjeForInneværendeBehandling)
                && LocalDate.now(clock).plusDays(28) >= forrigeSluttdato

    }

    // Det finnes en fremtidig periode med ordinær rett og gjenværende kvote
    fun harFremtidigRettOrdinær(
        vedtattSluttdato: LocalDate,
        rettighetstypeTidslinjeForInneværendeBehandling: Tidslinje<RettighetsType>
    ): Boolean {
        val varighetstidslinje = VarighetRegel().simluer(rettighetstypeTidslinjeForInneværendeBehandling)
        return varighetstidslinje.begrensetTil(Periode(vedtattSluttdato.plusDays(1), Tid.MAKS))
            .segmenter()
            .any { varighetSegment ->
                varighetSegment.verdi.brukerAvKvoter.any { kvote -> kvote == Kvote.ORDINÆR }
                        && varighetSegment.verdi !is Avslag
            }

    }

    private fun opprettNyBehandling(sak: Sak): SakOgBehandlingService.OpprettetBehandling =
        sakOgBehandlingService.finnEllerOpprettBehandling(
            sakId = sak.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                årsak = ÅrsakTilOpprettelse.UTVID_VEDTAKSLENGDE,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.UTVID_VEDTAKSLENGDE))
            ),
        )

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettBehandlingUtvidVedtakslengdeJobbUtfører(
                prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                sakRepository = repositoryProvider.provide(),
                underveisRepository = repositoryProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
                vilkårsresultatRepository = repositoryProvider.provide(),
                unleashGateway = gatewayProvider.provide(),
            )
        }

        override val type = "batch.UtvidVedtakslengdeJobbUtfører"

        override val navn = "Utvid vedtakslengde for saker"

        override val beskrivelse = "Skal trigge behandling som utvider vedtakslengde for saker som er i ferd med å nå sluttdato"

        /**
         * Kjøres hver dag kl 05:00
         */
        override val cron = CronExpression.createWithoutSeconds("0 5 * * *")
    }
}