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
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
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
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDate.now

class OpprettJobbUtvidVedtakslengdeJobbUtfører(
    private val underveisRepository: UnderveisRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val flytJobbRepository: FlytJobbRepository,
    private val unleashGateway: UnleashGateway,
    private val clock: Clock = Clock.systemDefaultZone()
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        // Forlenger behandlinger når det er 28 dager igjen til sluttdato
        val datoHvorSakerSjekkesForUtvidelse = now(clock).plusDays(28)

        val saker = hentKandidaterForUtvidelseAvVedtakslengde(datoHvorSakerSjekkesForUtvidelse)

        log.info("Fant ${saker.size} kandidater for utvidelse av vedtakslengde per $datoHvorSakerSjekkesForUtvidelse")
        if (unleashGateway.isEnabled(BehandlingsflytFeature.UtvidVedtakslengdeJobb)) {
            saker
                .filter { if (erDev()) it.id == 4243L else if (erProd()) it.id == 1100L else if (erLokal()) true else false}
                .also { log.info("Oppretter jobber for alle saker som er aktuelle kandidator for utvidelse av vedtakslengde. Antall = ${it.size}") }
                .forEach {
                    flytJobbRepository.leggTil(JobbInput(OpprettBehandlingUtvidVedtakslengdeJobbUtfører).forSak(it.toLong()))
                }
        }
    }

    private fun kunSakerUtenÅpneYtelsesbehandlinger(id: SakId): Boolean {
        val sisteBehandling = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(id)
        return sisteBehandling?.status() in setOf(Status.AVSLUTTET, Status.IVERKSETTES)
    }

    private fun harBehovForUtvidetVedtakslengde(
        behandlingId: BehandlingId,
        sakId: SakId,
        datoForUtvidelse: LocalDate
    ): Boolean {
        val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandlingId)
        if (underveisGrunnlag != null) {
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
            } else {
                log.info("Sak $sakId har ingen vedtatte underveisperioder")
            }
        } else {
            log.info("Sak $sakId har ingen underveisgrunnlag")
        }
        return false
    }

    fun skalUtvide(
        forrigeSluttdato: LocalDate,
        rettighetstypeTidslinjeForInneværendeBehandling: Tidslinje<RettighetsType>
    ): Boolean {
        return harFremtidigRettOrdinær(forrigeSluttdato, rettighetstypeTidslinjeForInneværendeBehandling)
                && now(clock).plusDays(28) >= forrigeSluttdato
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

    private fun hentKandidaterForUtvidelseAvVedtakslengde(dato: LocalDate): Set<SakId> {
        // TODO: Må filtrere vekk de som allerede har blitt kjørt, men ikke kvalifiserte til reell utvidelse av vedtakslengde
        return underveisRepository.hentSakerMedSisteUnderveisperiodeFørDato(dato)
            .filter { kunSakerUtenÅpneYtelsesbehandlinger(it) }
            .filter { kunSakerMedBehovForUtvidelseAvVedtakslengde(it, dato) }
            .toSet()
    }

    private fun kunSakerMedBehovForUtvidelseAvVedtakslengde(id: SakId, dato: LocalDate): Boolean {
        val sisteGjeldendeBehandling = sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(id)
        if (sisteGjeldendeBehandling != null) {
            return harBehovForUtvidetVedtakslengde(sisteGjeldendeBehandling.id, id, dato)
        }
        return false
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettJobbUtvidVedtakslengdeJobbUtfører(
                underveisRepository = repositoryProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
                flytJobbRepository = repositoryProvider.provide(),
                unleashGateway = gatewayProvider.provide(),
            )
        }

        override val type = "batch.OpprettJobbUtvidVedtakslengde"
        override val navn = "Opprett jobber for å utvide vedtakslengde for saker"
        override val beskrivelse = "Skal opprette jobber for å utvide vedtakslengde for saker"

        /**
         * Kjøres hver dag kl 05:00
         */
        override val cron = CronExpression.createWithoutSeconds("0 5 * * *")
    }
}