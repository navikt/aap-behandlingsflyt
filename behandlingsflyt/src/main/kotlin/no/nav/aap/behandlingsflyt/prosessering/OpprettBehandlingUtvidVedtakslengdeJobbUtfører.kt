package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeService
import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeUtvidelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
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

class OpprettBehandlingUtvidVedtakslengdeJobbUtfører(
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val behandlingService: BehandlingService,
    private val vedtakslengdeService: VedtakslengdeService,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())

        val sisteGjeldendeBehandling = behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId)
        if (sisteGjeldendeBehandling != null) {
            log.info("Gjeldende behandling for sak $sakId er ${sisteGjeldendeBehandling.id}")

            // Bruker sisteGjeldendeBehandling.id både for behandlingId og forrigeBehandlingId fordi vi ser på gjeldende behandling
            val vedtakslengdeUtvidelse = vedtakslengdeService.hentNesteVedtakslengdeUtvidelse(
                behandlingId = sisteGjeldendeBehandling.id,
                forrigeBehandlingId = sisteGjeldendeBehandling.id,
            )

            when (vedtakslengdeUtvidelse) {
                is VedtakslengdeUtvidelse.Automatisk -> {
                    log.info("Oppretter automatisk behandling for utvidelse ($vedtakslengdeUtvidelse) av vedtakslengde for sak $sakId")

                    val utvidVedtakslengdeBehandling = opprettNyBehandling(sakId, Vurderingsbehov.UTVID_VEDTAKSLENGDE)
                    prosesserBehandlingService.triggProsesserBehandling(utvidVedtakslengdeBehandling)
                }
                is VedtakslengdeUtvidelse.Manuell -> {
                    log.info("Oppretter manuell behandling for utvidelse ($vedtakslengdeUtvidelse) av vedtakslengde for sak $sakId")

                    val utvidVedtakslengdeBehandling = opprettNyBehandling(sakId, Vurderingsbehov.VEDTAKSLENGDE_MANUELT)
                    prosesserBehandlingService.triggProsesserBehandling(utvidVedtakslengdeBehandling)
                }
                is VedtakslengdeUtvidelse.IngenFremtidigBistandsbehovRettighet -> {
                    log.info("Sak med id $sakId har ingen fremtidig bistandsbehovrettighet, ingen behandling opprettes")
                }
            }
        } else {
            log.info("Sak med id $sakId har ingen gjeldende behandlinger, ingen behandling opprettes")
        }
    }

    private fun opprettNyBehandling(sakId: SakId, vurderingsbehov: Vurderingsbehov): BehandlingService.OpprettetBehandling =
        behandlingService.finnEllerOpprettBehandling(
            sakId = sakId,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                årsak = ÅrsakTilOpprettelse.UTVID_VEDTAKSLENGDE,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(vurderingsbehov))
            ),
        )

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettBehandlingUtvidVedtakslengdeJobbUtfører(
                prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
                vedtakslengdeService = VedtakslengdeService(repositoryProvider, gatewayProvider),
            )
        }

        override val type = "batch.UtvidVedtakslengdeJobbUtfører"
        override val navn = "Utvid vedtakslengde for saker"
        override val beskrivelse = "Skal trigge behandling som enten automatisk eller manuelt utvider " +
                "vedtakslengde for saker som er i ferd med å nå sluttdato"
    }
}