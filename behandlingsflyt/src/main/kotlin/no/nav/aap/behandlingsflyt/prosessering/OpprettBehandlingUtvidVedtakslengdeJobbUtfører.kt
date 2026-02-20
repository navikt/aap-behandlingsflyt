package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate.now

class OpprettBehandlingUtvidVedtakslengdeJobbUtfører(
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val vedtakslengdeService: VedtakslengdeService,
    private val clock: Clock = Clock.systemDefaultZone()
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val datoForUtvidelse = now(clock).plusDays(VedtakslengdeService.ANTALL_DAGER_FØR_UTVIDELSE)
        val sakId = SakId(input.sakId())

        val sisteGjeldendeBehandling = sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId)
        if (sisteGjeldendeBehandling != null) {
            log.info("Gjeldende behandling for sak $sakId er ${sisteGjeldendeBehandling.id}")
            // Bruker sisteGjeldendeBehandling.id både for behandlingId og forrigeBehandlingId fordi vi ser på gjeldende behandling
            if (vedtakslengdeService.skalUtvideSluttdato(sisteGjeldendeBehandling.id, sisteGjeldendeBehandling.id, datoForUtvidelse)) {
                log.info("Oppretter behandling for utvidelse av vedtakslengde for sak $sakId")
                val utvidVedtakslengdeBehandling = opprettNyBehandling(sakId)
                prosesserBehandlingService.triggProsesserBehandling(utvidVedtakslengdeBehandling)
            } else {
                log.info("Sak med id $sakId trenger ikke utvidelse av vedtakslengde, hopper over")
            }
        } else {
            log.info("Sak med id $sakId har ingen gjeldende behandlinger, hopper over")
        }
    }

    private fun opprettNyBehandling(sakId: SakId): SakOgBehandlingService.OpprettetBehandling =
        sakOgBehandlingService.finnEllerOpprettBehandling(
            sakId = sakId,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                årsak = ÅrsakTilOpprettelse.UTVID_VEDTAKSLENGDE,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.UTVID_VEDTAKSLENGDE))
            ),
        )

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettBehandlingUtvidVedtakslengdeJobbUtfører(
                prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
                vedtakslengdeService = VedtakslengdeService(repositoryProvider, gatewayProvider),
            )
        }

        override val type = "batch.UtvidVedtakslengdeJobbUtfører"
        override val navn = "Utvid vedtakslengde for saker"
        override val beskrivelse = "Skal trigge behandling som utvider vedtakslengde for saker som er i ferd med å nå sluttdato"
    }
}