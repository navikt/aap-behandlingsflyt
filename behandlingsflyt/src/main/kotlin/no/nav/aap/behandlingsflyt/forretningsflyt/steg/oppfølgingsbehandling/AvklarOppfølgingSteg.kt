package no.nav.aap.behandlingsflyt.forretningsflyt.steg.oppfølgingsbehandling

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.KonsekvensAvOppfølging
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.Ventebehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.HvemSkalFølgeOpp
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class AvklarOppfølgingSteg(
    private val oppfølgingsBehandlingRepository: OppfølgingsBehandlingRepository,
    private val behandlingService: BehandlingService,
    private val låsRepository: TaSkriveLåsRepository,
    private val prosesserBehandling: ProsesserBehandlingService,
    private val mottaDokumentService: MottaDokumentService,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val unleashGateway: UnleashGateway
) :
    BehandlingSteg {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val oppfølgingsoppgavedokument =
            requireNotNull(mottaDokumentService.hentOppfølgingsBehandlingDokument(kontekst.behandlingId)) {
                "Oppfølgingsoppgavedokument var null i steg ${type()}. BehandlingId: ${kontekst.behandlingId}"
            }
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = when (oppfølgingsoppgavedokument.hvemSkalFølgeOpp) {
                HvemSkalFølgeOpp.Lokalkontor -> Definisjon.AVKLAR_OPPFØLGINGSBEHOV_LOKALKONTOR
                HvemSkalFølgeOpp.NasjonalEnhet -> Definisjon.AVKLAR_OPPFØLGINGSBEHOV_NAY
            },
            vedtakBehøverVurdering = { true },
            erTilstrekkeligVurdert = { true },
            tilbakestillGrunnlag = { /* Flyten i oppfølgingsbehandling skal aldri tilbakeføres eller totrinnskontrolleres */ },
            kontekst = kontekst
        )
        håndterVurderingAvOppfølging(kontekst)

        if (!ventebehovVurdertFraFør(kontekst) && unleashGateway.isEnabled(BehandlingsflytFeature.OppfoelgingsoppgaveSynligMedEnGang)) {
            return FantVentebehov(
                ventebehov = Ventebehov(
                    definisjon = Definisjon.VENT_PÅ_OPPFØLGING_NY,
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER,
                    frist = oppfølgingsoppgavedokument.datoForOppfølging
                ),
            )
        }
        return Fullført
    }

    private fun ventebehovVurdertFraFør(
        kontekst: FlytKontekstMedPerioder,
    ): Boolean {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        // må sjekke på begge definisjonene for å håndtere eksisterende oppfølgingsoppgaver i det endringen går i prod.
        return avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.VENT_PÅ_OPPFØLGING_NY) || avklaringsbehovene.erVurdertTidligereIBehandlingen(
            Definisjon.VENT_PÅ_OPPFØLGING
        )
    }

    private fun håndterVurderingAvOppfølging(kontekst: FlytKontekstMedPerioder) {
        val grunnlag = oppfølgingsBehandlingRepository.hent(kontekst.behandlingId) ?: return
        when (grunnlag.konsekvensAvOppfølging) {
            KonsekvensAvOppfølging.INGEN -> {
                log.info("Ingen konsekvens av oppfølging. Avslutter oppfølgingsbehandling for sak ${kontekst.sakId}.")
            }

            KonsekvensAvOppfølging.OPPRETT_VURDERINGSBEHOV -> {
                val vurderingsbehov = grunnlag.opplysningerTilRevurdering
                log.info("Oppretter ny behandling med vurderingsbehov $vurderingsbehov for sak ${kontekst.sakId}.")
                val behandling = behandlingService.finnEllerOpprettOrdinærBehandling(
                    sakId = kontekst.sakId,
                    vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                        årsak = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE,
                        vurderingsbehov = vurderingsbehov.map { VurderingsbehovMedPeriode(it) },
                        beskrivelse = grunnlag.årsak
                    ),
                )

                val behandlingSkrivelås =
                    låsRepository.låsBehandling(behandling.id)

                prosesserBehandling.triggProsesserBehandling(behandling.sakId, behandling.id)
                låsRepository.verifiserSkrivelås(behandlingSkrivelås)
            }
        }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return AvklarOppfølgingSteg(
                oppfølgingsBehandlingRepository = repositoryProvider.provide(),
                behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
                låsRepository = repositoryProvider.provide(),
                prosesserBehandling = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                mottaDokumentService = MottaDokumentService(repositoryProvider.provide()),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
                avklaringsbehovRepository = repositoryProvider.provide(),
                unleashGateway = gatewayProvider.provide()
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_OPPFØLGING
        }
    }
}