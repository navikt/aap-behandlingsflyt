package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory
import java.time.LocalDate

class TriggBarnetilleggSatsJobbUtfører(
    val sakRepository: SakRepository,
    private val sakOgBehandlingService: SakOgBehandlingService
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {

        val saker = sakRepository.finnSakerMedBarnetillegg(LocalDate.of(2026, 1, 1))
        log.info("Fant ${saker.size} saker med barnetillegg.")

        saker.forEach {
            sakOgBehandlingService.finnEllerOpprettBehandling(
                it, VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.BARNETILLEGG_SATS_REGULERING)),
                    årsak = ÅrsakTilOpprettelse.BARNETILLEGG_SATSENDRING
                )
            )
        }

    }


    companion object : ProvidersJobbSpesifikasjon {
        override val type = "satsendring"
        override val navn = "Finn og prosesser saker med barnetillegg."
        override val beskrivelse = ""

        /**
         * Kjør hver 2 januar kl 09:00.
         */
        override val cron: CronExpression = CronExpression.create("0 9 2 1 *")

        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): TriggBarnetilleggSatsJobbUtfører {
            return TriggBarnetilleggSatsJobbUtfører(
                sakRepository = repositoryProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider)
            )
        }
    }
}