package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.KanTriggeRevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreInformasjonskrav.UføreRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Year

class UføreInformasjonskrav(
    private val sakService: SakService,
    private val uføreRepository: UføreRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val uføreRegisterGateway: UføreRegisterGateway,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav<UføreInformasjonskrav.UføreInput, UføreRegisterdata>, KanTriggeRevurdering {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakService = SakService(repositoryProvider),
        uføreRepository = repositoryProvider.provide(),
        beregningVurderingRepository = repositoryProvider.provide(),
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
        return kontekst.erFørstegangsbehandlingEllerRevurdering()
                && !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
                && (oppdatert.ikkeKjørtSisteKalenderdag() || kontekst.rettighetsperiode != oppdatert?.rettighetsperiode)
        // TODO: endring i ytterligereNedsettelsesDato bør trigge ny innhenting?
    }

    data class UføreInput(val sak: Sak, val behandlingId: BehandlingId) : InformasjonskravInput

    data class UføreRegisterdata(val innhentMedHistorikk: List<Uføre>) : InformasjonskravRegisterdata

    override fun klargjør(kontekst: FlytKontekstMedPerioder): UføreInput {
        return UføreInput(sakOgBehandlingService.hentSakFor(kontekst.behandlingId), kontekst.behandlingId)
    }

    override fun hentData(input: UføreInput): UføreRegisterdata {
        return UføreRegisterdata(hentUføregrader(input.behandlingId))
    }

    override fun oppdater(
        input: UføreInput,
        registerdata: UføreRegisterdata,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        log.info("Oppdaterer uførehistorikk for behandlingen")
        val behandlingId = kontekst.behandlingId
        val uføregrader = registerdata.innhentMedHistorikk

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
        val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
        // prøver å sette fraDato riktig hvis den finnes
        val fraDato = beregningVurdering?.tidspunktVurdering?.ytterligereNedsattArbeidsevneDato
            ?: beregningVurdering?.tidspunktVurdering?.nedsattArbeidsevneDato
            ?: sak.rettighetsperiode.fom
        return uføreRegisterGateway.innhentMedHistorikk(sak.person, treÅrFør(fraDato))
    }

    private fun treÅrFør(fraOgMed: LocalDate): LocalDate {
        return Year.from(fraOgMed).minusYears(3).atDay(1)
    }

    override fun behovForRevurdering(behandlingId: BehandlingId): List<VurderingsbehovMedPeriode> {
        val uføregrader = hentUføregrader(behandlingId)
        val eksisterendeGrunnlag = uføreRepository.hentHvisEksisterer(behandlingId)

        // Ønsker ikke trigge revurdering automatisk i dette tilfellet enn så lenge
        val gikkFraNullTilTomtGrunnlag = uføregrader.isEmpty() && eksisterendeGrunnlag == null

        return if (harEndringerUføre(eksisterendeGrunnlag, uføregrader) && !gikkFraNullTilTomtGrunnlag) {
            listOf(VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_SAMORDNING_UFØRE))
        } else {
            emptyList()
        }
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.UFØRE

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): UføreInformasjonskrav {
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