package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.KanTriggeRevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSiste
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Duration

class UføreInformasjonskrav(
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val uføreRepository: UføreRepository,
    private val uføreRegisterGateway: UføreRegisterGateway,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav, KanTriggeRevurdering {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
        uføreRepository = repositoryProvider.provide(),
        uføreRegisterGateway = gatewayProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    override val navn = Companion.navn
    private val log = LoggerFactory.getLogger(javaClass)

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                oppdatert.ikkeKjørtSiste(Duration.ofHours(1)) &&
                !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        log.info("Oppdaterer uførehistorikk for behandlingen")
        val behandlingId = kontekst.behandlingId
        val uføregrader = hentUføregrader(behandlingId)
        val eksisterendeGrunnlag = uføreRepository.hentHvisEksisterer(behandlingId)

        if (harEndringerUføre(eksisterendeGrunnlag, uføregrader)) {
            log.info("Fant endringer i uførehistorikk for behandlingen")
            uføreRepository.lagre(behandlingId, uføregrader)
            return ENDRET
        }

        return IKKE_ENDRET
    }

    private fun hentUføregrader(behandlingId: BehandlingId): List<Uføre> {
        val sak = sakOgBehandlingService.hentSakFor(behandlingId)
        return uføreRegisterGateway.innhentMedHistorikk(sak.person, sak.rettighetsperiode.fom)
    }

    override fun behovForRevurdering(behandlingId: BehandlingId): List<VurderingsbehovMedPeriode> {
        val uføregrader = hentUføregrader(behandlingId)
        val eksisterendeGrunnlag = uføreRepository.hentHvisEksisterer(behandlingId)

        // Ønsker ikke trigge revurdering automatisk i dette tilfellet enn så lenge
        val gikkFraNullTilTomtGrunnlag = uføregrader.isEmpty() && eksisterendeGrunnlag == null
        
        return if (harEndringerUføre(eksisterendeGrunnlag, uføregrader) && !gikkFraNullTilTomtGrunnlag) {
            listOf(VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_SAMORDNING))
        } else {
            emptyList()
        }
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.UFØRE

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): UføreInformasjonskrav {
            return UføreInformasjonskrav(repositoryProvider, gatewayProvider)
        }

        fun harEndringerUføre(
            eksisterende: UføreGrunnlag?,
            uføregrader: List<Uføre>
        ): Boolean {
            return if (eksisterende == null) {
                uføregrader.isNotEmpty()
            } else {
                uføregrader.toSet() != eksisterende.vurderinger.toSet()
            }
        }
    }
}