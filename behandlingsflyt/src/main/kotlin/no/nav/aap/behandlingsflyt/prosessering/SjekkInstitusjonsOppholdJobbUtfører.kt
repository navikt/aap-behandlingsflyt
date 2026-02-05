package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
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
import kotlin.collections.all

class SjekkInstitusjonsOppholdJobbUtfører(
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val sakRepository: SakRepository,
    private val institusjonsOppholdRepository: InstitusjonsoppholdRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val trukketSøknadService: TrukketSøknadService,
    private val behandlingRepository: BehandlingRepository,
    private val underveisgrunnlagRepository: UnderveisRepository,
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

                    val sisteYtelsesBehandling = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sak.id)

                    if (sisteYtelsesBehandling != null) {
                        val sak = sakRepository.hent(sak.id)
                        log.info("Gjeldende behandling for sak $sak.id (${sak.saksnummer}) er ${sisteYtelsesBehandling.id}")
                        if (erKandidatForVurderingAvInstitusjonsopphold(sisteYtelsesBehandling.id)) {
                            val vurderingsbehovOgÅrsaker =
                                behandlingRepository.hentVurderingsbehovOgÅrsaker(sisteYtelsesBehandling.id)
                            val underveisgrunnlag =
                                underveisgrunnlagRepository.hentHvisEksisterer(sisteYtelsesBehandling.id)
                            if (underveisgrunnlag == null) {
                                log.info("Finner ikke underveisgrunnlag for behandlingId ${sisteYtelsesBehandling.id}")
                            } else {
                                val søknadErTrukket = trukketSøknadService.søknadErTrukket(sisteYtelsesBehandling.id)
                                if (søknadErTrukket) {
                                    log.info("Institusjonsopphold oppdateres ikke, da sak med $sak.id er trukket")
                                } else {
                                    val alleIkkeOppfylt =
                                        underveisgrunnlag
                                            .somTidslinje()
                                            .segmenter()
                                            .all { it.verdi.utfall == Utfall.IKKE_OPPFYLT }
                                    if (vurderingsbehovOgÅrsaker.any { it.vurderingsbehov.any { vurderingsbehovMedPeriode -> vurderingsbehovMedPeriode.type == Vurderingsbehov.INSTITUSJONSOPPHOLD } }) {
                                        log.info("Vurderingsbehov for institusjonsopphold finnes allerede")
                                    } else if (alleIkkeOppfylt) {
                                        log.info("Vurderingsbehov for institusjonsopphold opprettes ikke, da det er avslag overalt")
                                    } else {
                                        log.info("Fant sak med institusjonsopphold $sak.id")
                                        val opprettInstitusjonsOppholdBehandling = opprettNyBehandling(sak)
                                        log.info("Opprettet behandling for instopphold for ${opprettInstitusjonsOppholdBehandling.id} og ${opprettInstitusjonsOppholdBehandling.forrigeBehandlingId}")
                                        prosesserBehandlingService.triggProsesserBehandling(
                                            opprettInstitusjonsOppholdBehandling
                                        )
                                        log.info("Ferdig med å trigge instopphold for $sak.id")
                                    }
                                }
                            }
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
            if (tomErIFremtidenOgInnenTreMaaneder(opphold.periode)) {
                log.info("For behandlingsid $behandlingId er oppholdene true")
                return true
            }
            log.info("For behandlingsid $behandlingId er oppholdene false")
        }
        return false
    }

    private fun tomErIFremtidenOgInnenTreMaaneder(periode: Periode): Boolean {
        return periode.tom.isBefore(LocalDate.now().withDayOfMonth(1).plusMonths(4)) && periode.tom.isAfter(LocalDate.now())
    }

    private fun opprettNyBehandling(sak: Sak): Behandling =
        sakOgBehandlingService.finnEllerOpprettOrdinærBehandling(
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
                trukketSøknadService = TrukketSøknadService(repositoryProvider),
                behandlingRepository = repositoryProvider.provide(),
                underveisgrunnlagRepository = repositoryProvider.provide(),
                unleashGateway = gatewayProvider.provide(),
            )
        }

        override val type = "batch.InstitusjonsOppholdJobbUtfører"

        override val navn = "Sjekker om institusjonsopphold skal vurderes"

        override val beskrivelse = "Skal trigge behandling som vurderer institusjonsopphold"

        /**
         * Kjøres hver time enn så lenge, slås av og på med Feature Toggle
         */
        // override val cron = CronExpression.createWithoutSeconds("0 * * * *")
        override val cron = CronExpression.createWithoutSeconds("*/5 * * * *")
    }
}