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

class OpprettBehandlingUtvidVedtakslengdeJobbUtfører(
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val sakRepository: SakRepository,
    private val underveisRepository: UnderveisRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val unleashGateway: UnleashGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val dryRun = true
        val datoForUtvidelse = LocalDate.now().plusDays(28)

        val saker = hentKandidaterForUtvidelseAvVedtakslengde(datoForUtvidelse)
        log.info("Fant ${saker.size} kandidater for utvidelse av vedtakslende per $datoForUtvidelse")

        if (unleashGateway.isEnabled(BehandlingsflytFeature.UtvidVedtakslengde)) {
            saker
                // TODO .filter { it.id == ? } // Kun kjøre spesifikk sak i første runde
                .forEach { sakId ->
                    try {
                        val sisteGjeldendeBehandling = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sakId)
                        if (sisteGjeldendeBehandling != null) {
                            log.info("Gjeldende behandling for sak $sakId er ${sisteGjeldendeBehandling.id}")

                            // Utvider rettighetsperiode til Tid.MAKS dersom denne har en annen verdi - tom her skal
                            // på sikt fases ut og denne koden kan fjernes når alle saker har Tid.MAKS som tom
                            val sak = sakRepository.hent(sakId)
                            if (sak.rettighetsperiode.tom != Tid.MAKS) {
                                log.info("Utvider rettighetsperiode fra ${sak.rettighetsperiode.tom} til ${Tid.MAKS} for saksnummer ${sak.saksnummer}")
                                if (!dryRun) sakRepository.oppdaterRettighetsperiode(sak.id, Periode(sak.rettighetsperiode.fom, Tid.MAKS))
                            }

                            // Trigger behandling som utvider vedtakslengde dersom nødvendig
                            val underveisGrunnlag = underveisRepository.hentHvisEksisterer(sisteGjeldendeBehandling.id)
                            if (underveisGrunnlag != null && harBehovForUtvidetVedtakslengde(underveisGrunnlag, datoForUtvidelse)) {
                                log.info("Oppretter behandling for utvidelse av vedtakslengde for saksnummer ${sak.saksnummer}")
                                if (!dryRun) {
                                    val utvidVedtakslengdeBehandling = opprettNyBehandling(sak)
                                    prosesserBehandlingService.triggProsesserBehandling(utvidVedtakslengdeBehandling)
                                }
                            } else {
                                log.info("Sak med id $sakId trenger ikke utvidelse av vedtakslengde, hopper over")
                            }

                        } else {
                            log.info("Sak med id $sakId har ingen gjeldende behandlinger, hopper over")
                        }
                    } catch (e: Exception) {
                        log.error("Feilet ved utvidelse av vedtakslengde for sak $sakId", e)
                    }
                }
        }
    }

    private fun harBehovForUtvidetVedtakslengde(
        underveisGrunnlag: UnderveisGrunnlag,
        datoForUtvidelse: LocalDate
    ): Boolean {
        val harFremtidigRettOrdinær = true // TODO sjekk om det finnes rett i fremtiden av type ORDINÆR
        val sisteVedtatteUnderveisperiode = underveisGrunnlag.perioder.maxBy { it.periode.tom }
        val gjeldendeSluttdato = sisteVedtatteUnderveisperiode.periode.tom
        return gjeldendeSluttdato.isBefore(datoForUtvidelse) && harFremtidigRettOrdinær
    }

    private fun hentKandidaterForUtvidelseAvVedtakslengde(dato: LocalDate): Set<SakId> {
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