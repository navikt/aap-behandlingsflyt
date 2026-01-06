package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory

class TriggBarnetilleggSatsJobbUtfører(
    val sakRepository: SakRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val prosesserBehandlingService: ProsesserBehandlingService,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())

        log.info("Oppretter satsendring-behandling for sak med id $sakId.")

        if (OpprettJobbForTriggBarnetilleggSatsJobbUtfører.erAktiv) {
            log.info("Trigger behandlinger for barnetillegg sats regulering.")

            val behandling = sakOgBehandlingService.finnEllerOpprettBehandling(
                sakId, VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.BARNETILLEGG_SATS_REGULERING)),
                    årsak = ÅrsakTilOpprettelse.BARNETILLEGG_SATSENDRING
                )
            )
            prosesserBehandlingService.triggProsesserBehandling(behandling)

        } else {
            log.info("Trigger ikke behandlinger for barnetillegg sats regulering. Jobb er ikke aktiv.")
        }
    }


    companion object : ProvidersJobbSpesifikasjon {
        override val type = "satsendring"
        override val navn = "Prosesser sak med barnetillegg."
        override val beskrivelse = "Input: sak-id"

        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): TriggBarnetilleggSatsJobbUtfører {
            return TriggBarnetilleggSatsJobbUtfører(
                sakRepository = repositoryProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
                prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
            )
        }
    }
}