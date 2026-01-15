package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
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
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDate.now

class OpprettBehandlingUtvidVedtakslengdeJobbUtfører(
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val sakRepository: SakRepository,
    private val underveisRepository: UnderveisRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val unleashGateway: UnleashGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val dryRun = false

        // Forlenger behandlinger når det er 28 dager igjen til sluttdato
        val datoHvorSakerSjekkesForUtvidelse = now().plusDays(28)

        val saker = hentKandidaterForUtvidelseAvVedtakslengde(datoHvorSakerSjekkesForUtvidelse)
        log.info("Fant ${saker.size} kandidater for utvidelse av vedtakslende per $datoHvorSakerSjekkesForUtvidelse (dryRun=$dryRun)")

        if (unleashGateway.isEnabled(BehandlingsflytFeature.UtvidVedtakslengdeJobb)) {
            val resultat = saker
                // TODO .filter { it.id == ? } // Kun kjøre spesifikk sak i første runde
                .map { sakId ->
                    val sisteGjeldendeBehandling = sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId)
                    if (sisteGjeldendeBehandling != null) {
                        log.info("Gjeldende behandling for sak $sakId er ${sisteGjeldendeBehandling.id}")
                        val sak = sakRepository.hent(sakId)

                        // Trigger behandling som utvider vedtakslengde dersom nødvendig
                        val underveisGrunnlag = underveisRepository.hentHvisEksisterer(sisteGjeldendeBehandling.id)
                        if (underveisGrunnlag != null && (harBehovForUtvidetVedtakslengde(sakId, underveisGrunnlag, datoHvorSakerSjekkesForUtvidelse) || sak.rettighetsperiode.tom != Tid.MAKS)) {

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

    private fun harBehovForUtvidetVedtakslengde(
        sakId: SakId,
        underveisGrunnlag: UnderveisGrunnlag,
        datoForUtvidelse: LocalDate
    ): Boolean {

        val harFremtidigRettOrdinær = true // TODO sjekk om det finnes rett i fremtiden av type ORDINÆR
        val sisteVedtatteUnderveisperiode = underveisGrunnlag.perioder.maxByOrNull { it.periode.tom }
        if (sisteVedtatteUnderveisperiode != null) {
            val gjeldendeSluttdato = sisteVedtatteUnderveisperiode.periode.tom

            log.info("Sak $sakId har harFremtidigRettOrdinær=$harFremtidigRettOrdinær og gjeldendeSluttdato=$gjeldendeSluttdato")
            return gjeldendeSluttdato.isBefore(datoForUtvidelse) && harFremtidigRettOrdinær
        }
        log.info("Sak $sakId har harFremtidigRettOrdinær=$harFremtidigRettOrdinær men har ingen vedtatte underveisperioder")
        return false
    }

    private fun hentKandidaterForUtvidelseAvVedtakslengde(dato: LocalDate): Set<SakId> {
        // TODO: Må filtrere vekk de som allerede har blitt kjørt, men ikke kvalifiserte til reell utvidelse av vedtakslengde
        return underveisRepository.hentSakerMedSisteUnderveisperiodeFørDato(dato)
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