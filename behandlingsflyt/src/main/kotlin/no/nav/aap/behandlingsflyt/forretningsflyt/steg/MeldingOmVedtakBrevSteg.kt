package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.brev.BarnetilleggSatsRegulering
import no.nav.aap.behandlingsflyt.behandling.brev.BrevBehov
import no.nav.aap.behandlingsflyt.behandling.brev.BrevUtlederService
import no.nav.aap.behandlingsflyt.behandling.brev.KlageOpprettholdelse
import no.nav.aap.behandlingsflyt.behandling.brev.UtvidVedtakslengde
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.OpprettJobbForTriggBarnetilleggSatsJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status as BrevStatus

private val log = LoggerFactory.getLogger("BrevSteg")

class MeldingOmVedtakBrevSteg(
    private val brevUtlederService: BrevUtlederService,
    private val brevbestillingService: BrevbestillingService,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val trekkKlageService: TrekkKlageService,
    private val avbrytAktivitetspliktbehandlingService: AvbrytAktivitetspliktbehandlingService,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        brevUtlederService = BrevUtlederService(repositoryProvider, gatewayProvider),
        brevbestillingService = BrevbestillingService(repositoryProvider, gatewayProvider),
        behandlingRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        trekkKlageService = TrekkKlageService(repositoryProvider),
        avbrytAktivitetspliktbehandlingService = AvbrytAktivitetspliktbehandlingService(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandlingErAvbrutt =
            avbrytAktivitetspliktbehandlingService.behandlingErAvbrutt(kontekst.behandlingId)
                    || trekkKlageService.klageErTrukket(kontekst.behandlingId)
        val brevBehov = brevUtlederService.utledBehovForMeldingOmVedtak(kontekst.behandlingId)
        val harBestillingOmVedtakBrev = brevbestillingService.harBestillingOmVedtak(kontekst.behandlingId)

        listOf(
            Definisjon.SKRIV_VEDTAKSBREV,
            Definisjon.SKRIV_VEDTAKSBREV_SAKSBEHANDLER
        ).forEach { definisjon ->
            avklaringsbehovService.oppdaterAvklaringsbehov(
                definisjon,
                vedtakBehøverVurdering = {
                    vedtakBehøverVurdering(
                    kontekst.behandlingId,
                        avklaringsbehovRepository.hentAvklaringsbehovene(
                            kontekst.behandlingId
                        ), behandlingErAvbrutt, definisjon, brevBehov
                    )
                },
                erTilstrekkeligVurdert =
                    { brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(kontekst.behandlingId) },
                tilbakestillGrunnlag = { tilbakestillGrunnlag(kontekst.behandlingId) },
                kontekst
            )
        }

        if (brevBehov != null && !behandlingErAvbrutt && !harBestillingOmVedtakBrev) {
            bestillBrev(kontekst, brevBehov)
        }
        return Fullført
    }

    /**
     * Brevbestillinger i endeTilstand (FULLFØRT, AVBRUTT) kan per i dag ikke tilbakestilles i aap-behandlingsflyt.
     * Hvis dette endres i fremtiden må også tilbakestillGrunnlag() logikken her tilpasses.
     *
     * BrevBestillinger i tilstand FORHÅNDSVISNING_KLAR og SENDT kan i teorien tilbakestilles. Den praktiske
     * begrensningen per i dag er at selve brev steget ikke kan tilbakestilles (ingen fremtidige scenarior for dette foreløpig)
     */
    private fun tilbakestillGrunnlag(behandlingId: BehandlingId) {
        if (!brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)) {
            val brevBestillingerOmVedtakSomKanTilbakestilles =
                brevbestillingService.hentTilbakestillbareBestillingerOmVedtak(behandlingId)
            for (brevBestilling in brevBestillingerOmVedtakSomKanTilbakestilles) {
                brevbestillingService.avbryt(brevBestilling.behandlingId, brevBestilling.referanse)
            }
        }
    }

    private fun vedtakBehøverVurdering(
        behandlingId: BehandlingId,
        avklaringsbehovene: Avklaringsbehovene,
        behandlingErAvbrutt: Boolean,
        forDefinisjon: Definisjon,
        brevBehov: BrevBehov?
    ): Boolean {
        val harManueltBrevbehov = (!behandlingErAvbrutt && brevBehov != null && !brevBehov.typeBrev.erAutomatiskBrev())
        
        val behovForBeslutterbrev = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.SKRIV_VEDTAKSBREV)
        val harBeslutterSkrevetBrev = behovForBeslutterbrev?.historikk?.any { it.status == Status.AVSLUTTET } ?: false
        
        val erBeslutterbehovAvbrutt = behovForBeslutterbrev?.status() == Status.AVBRUTT
        
        if (harBeslutterSkrevetBrev && erBeslutterbehovAvbrutt && brevBehov != null) {
            val brevbestilling = brevbestillingService.hentBestillinger(behandlingId, brevBehov.typeBrev)
                .maxByOrNull { it.opprettet }
            if (brevbestilling?.status == BrevStatus.AVBRUTT) {
                brevbestillingService.gjenopptaBestilling(behandlingId, brevbestilling.referanse)
            }
        }
        
        return when (brevBehov) {
            is KlageOpprettholdelse -> !harBeslutterSkrevetBrev && harManueltBrevbehov && forDefinisjon == Definisjon.SKRIV_VEDTAKSBREV_SAKSBEHANDLER
            else -> harManueltBrevbehov && forDefinisjon == Definisjon.SKRIV_VEDTAKSBREV
        }
    }

    private fun bestillBrev(kontekst: FlytKontekstMedPerioder, brevBehov: BrevBehov) {
        val automatisk = brevBehov.typeBrev.erAutomatiskBrev()
        log.info("Bestiller${if (automatisk) " automatisk" else ""} brev for behandling ${kontekst.behandlingId}.")
        brevbestillingService.bestill(
            behandlingId = kontekst.behandlingId,
            brevBehov = brevBehov,
            unikReferanse = unikReferanse(brevBehov, kontekst),
            ferdigstillAutomatisk = automatisk,
            brukApiV3 = brukApiV3(kontekst.behandlingId, brevBehov.typeBrev)
        )
    }

    private fun unikReferanse(brevBehov: BrevBehov, kontekst: FlytKontekstMedPerioder): String {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        return when (brevBehov) {
            is BarnetilleggSatsRegulering -> {
                val sak = sakRepository.hent(kontekst.sakId)
                "${sak.saksnummer}-${brevBehov.typeBrev}-${OpprettJobbForTriggBarnetilleggSatsJobbUtfører.jobbKonfigurasjon.unikBrevreferanseForSak}"
            }

            is UtvidVedtakslengde -> {
                val sak = sakRepository.hent(kontekst.sakId)
                "${sak.saksnummer}-${brevBehov.typeBrev}-${brevBehov.sisteDagMedYtelse.format(DateTimeFormatter.ISO_DATE)}"
            }

            else -> {
                "${behandling.referanse}-${brevBehov.typeBrev}"
            }
        }
    }

    private fun brukApiV3(behandlingId: BehandlingId, typeBrev: TypeBrev): Boolean {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FATTE_VEDTAK) ?: return false
        val endretAv = avklaringsbehov.endretAv()
        return unleashGateway.isEnabled(BehandlingsflytFeature.NyBrevbyggerV3, endretAv, typeBrev)
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return MeldingOmVedtakBrevSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.BREV
        }
    }
}