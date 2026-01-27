package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory
import java.time.LocalDate

class SjekkInstitusjonsOppholdJobbUtfører(
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val sakRepository: SakRepository,
    private val institusjonsOppholdRepository: InstitusjonsoppholdRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val unleashGateway: UnleashGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {

        //Logikk for hvilke institusjonsopphold som skal legges til
        val sakerMedInstitusjonsOpphold = sakRepository.finnSakerMedInstitusjonsOpphold()

        log.info("Fant ${sakerMedInstitusjonsOpphold.size} kandidater for institusjonsopphold")


        if (unleashGateway.isEnabled(BehandlingsflytFeature.InstitusjonsoppholdJobb)) {
            val resultat = sakerMedInstitusjonsOpphold
                .map { sak ->
                    val sisteGjeldendeBehandling = sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sak.id)
                    //TODO får vel kanskje sjekke vurderingsbehov, om det finnes fra før
                    if (sisteGjeldendeBehandling != null) {
                        val sak = sakRepository.hent(sak.id)
                        log.info("Gjeldende behandling for sak $sak.id (${sak.saksnummer}) er ${sisteGjeldendeBehandling.id}")
                        if (erKandidatForVurderingAvInstitusjonsopphold(sisteGjeldendeBehandling.id)) {
                            val opprettInstitusjonsOppholdBehandling = opprettNyBehandling(sak)
                            log.info("Fant sak med institusjonsopphold $sak.id")
                            prosesserBehandlingService.triggProsesserBehandling(opprettInstitusjonsOppholdBehandling)
                        }
                    } else {
                        log.info("Sak med id $sak.id har ikke behandling, hopper over")
                    }

                }

            log.info("Jobb for sjekk av institusjonsopphold fullført for ${resultat.count()} av ${sakerMedInstitusjonsOpphold.size} saker")
        }
    }


    private fun erKandidatForVurderingAvInstitusjonsopphold(behandlingId: BehandlingId): Boolean {

        val grunnlag = institusjonsOppholdRepository.hentHvisEksisterer(behandlingId)
        grunnlag?.oppholdene?.opphold?.forEach { opphold ->
            if (tomErInnenTreMaaneder(opphold.periode))
                return true
        }
        return false
    }

    private fun tomErInnenTreMaaneder(periode: Periode): Boolean {
        return periode.tom.isBefore(LocalDate.now().withDayOfMonth(1).plusMonths(4))
    }

    private fun opprettNyBehandling(sak: Sak): SakOgBehandlingService.OpprettetBehandling =
        sakOgBehandlingService.finnEllerOpprettBehandling(
            sakId = sak.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                årsak = ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.INSTITUSJONSOPPHOLD))
            ),
        )

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return SjekkInstitusjonsOppholdJobbUtfører(
                prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                sakRepository = repositoryProvider.provide(),
                institusjonsOppholdRepository = repositoryProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
                unleashGateway = gatewayProvider.provide(),
            )
        }

        override val type = "batch.InstitusjonsOppholdJobbUtfører"

        override val navn = "Sjekker om institusjonsopphold skal vurderes"

        override val beskrivelse = "Skal trigge behandling som vurderer institusjonsopphold"

        /**
         * Kjøres hver time enn så lenge, slås av og på med Feature Toggle
         */
        override val cron = CronExpression.createWithoutSeconds("0 * * * *")
    }
}