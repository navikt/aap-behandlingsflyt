package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class OpprettBehandlingFastsattPeriodePassertJobbUtfører(
    private val sakService: SakService,
    private val underveisRepository: UnderveisRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val prosesserBehandlingService: ProsesserBehandlingService,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val sak = sakService.hent(SakId(input.sakId()))

        if (!sak.rettighetsperiode.inneholder(LocalDate.now())) {
            return
        }

        val behandling = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sak.id) ?: return

        if (behandling.status().erÅpen() && Vurderingsbehov.FASTSATT_PERIODE_PASSERT in behandling.vurderingsbehov()
                .map { it.type }
        ) {
            log.info("Det finnes allerede en kjørende jobb av årsak FASTSATT_PERIODE_PASSERT. Avbryter.")
            return
        }

        val underveisperioder = underveisRepository.hentHvisEksisterer(behandling.id)
            ?.perioder

        if (underveisperioder == null) {
            log.info("Fant ikke underveisperioder for behandling med id ${behandling.id}. Avbryter.")
            return
        }

        val førsteAntatteMeldeperiode = underveisperioder
            .filter { it.meldepliktStatus == MeldepliktStatus.FREMTIDIG_OPPFYLT }
            .minByOrNull { it.meldePeriode }

        if (førsteAntatteMeldeperiode == null) {
            log.info("Fant ikke førsteAntatteMeldeperiode. Avbryter.")
            return
        }

        val tidspunktForKjøringVedManglendeMeldekort =
            førsteAntatteMeldeperiode.meldePeriode.fom.plusDays(8).atStartOfDay().plusHours(2)

        if (LocalDateTime.now() < tidspunktForKjøringVedManglendeMeldekort) {
            log.info("Jobben ble kjørt før tidspunktForKjøringVedManglendeMeldekort. Avbryter.")
            return
        }

        val fastsattPeriodePassertBehandling = sakOgBehandlingService.finnEllerOpprettBehandling(
            sakId = sak.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                årsak = ÅrsakTilOpprettelse.FASTSATT_PERIODE_PASSERT,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.FASTSATT_PERIODE_PASSERT))
            )
        )

        prosesserBehandlingService.triggProsesserBehandling(fastsattPeriodePassertBehandling)
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettBehandlingFastsattPeriodePassertJobbUtfører(
                sakService = SakService(repositoryProvider),
                underveisRepository = repositoryProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
                prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
            )
        }

        override val type = "batch.OpprettBehandlingFastsattPeriodePassert"
        override val navn = "Opprett behandling fordi fastsatt dag er passert"
        override val beskrivelse =
            """
                Starter ny behandling hvis siste behandlig har antatt at meldeplikten er oppfylt, men
                fastsatt dag er passert, og meldekort nå mangler.
            """.trimIndent()
    }
}
