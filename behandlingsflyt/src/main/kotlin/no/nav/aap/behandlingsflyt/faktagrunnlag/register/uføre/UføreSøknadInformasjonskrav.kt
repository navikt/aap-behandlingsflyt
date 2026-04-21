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
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdagForBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class UføreSøknadInformasjonskrav(
    private val sakService: SakService,
    private val uføreSøknadRepository: UføreSøknadRepository,
    private val uføreRegisterGateway: UføreRegisterGateway,
    private val tidligereVurderinger: TidligereVurderinger,
    private val unleashGateway: UnleashGateway,
) : Informasjonskrav<UføreSøknadInformasjonskrav.UføreSøknadInput, UføreSøknadInformasjonskrav.UføreSøknadRegisterdata>,
    KanTriggeRevurdering {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakService = SakService(repositoryProvider, gatewayProvider),
        uføreSøknadRepository = repositoryProvider.provide(),
        uføreRegisterGateway = gatewayProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        unleashGateway = gatewayProvider.provide(),
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
                && (oppdatert.ikkeKjørtSisteKalenderdagForBehandling(kontekst.behandlingId) || kontekst.rettighetsperiode != oppdatert?.rettighetsperiode || kontekst.erVurderingsbehovEndretEtterOppdatertInformasjonskrav(
            oppdatert
        ))
    }

    data class UføreSøknadInput(val sak: Sak, val behandlingId: BehandlingId) : InformasjonskravInput

    data class UføreSøknadRegisterdata(val uføreSøknad: UføreSøknad?) : InformasjonskravRegisterdata

    override fun klargjør(kontekst: FlytKontekstMedPerioder): UføreSøknadInput {
        val behandlingId = kontekst.behandlingId
        return UføreSøknadInput(sakService.hentSakFor(behandlingId), behandlingId)
    }

    override fun hentData(input: UføreSøknadInput): UføreSøknadRegisterdata {
        return UføreSøknadRegisterdata(hentUføreSøknad(input))
    }

    override fun oppdater(
        input: UføreSøknadInput,
        registerdata: UføreSøknadRegisterdata,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        log.info("Oppdaterer uføresøknad for behandlingen")
        val behandlingId = kontekst.behandlingId
        val uføreSøknad = registerdata.uføreSøknad

        val eksisterendeGrunnlag = uføreSøknadRepository.hentHvisEksisterer(behandlingId)

        if (uføreSøknad != null && harEndringerIUføreSøknad(eksisterendeGrunnlag, uføreSøknad)) {
            log.info("Fant endringer i uføresøknad for behandlingen")
            uføreSøknadRepository.lagre(behandlingId, uføreSøknad)
            return ENDRET
        }

        return IKKE_ENDRET
    }

    private fun hentUføreSøknad(uføreInput: UføreSøknadInput): UføreSøknad? {
        return uføreRegisterGateway.hentÅpenUføreSøknad(uføreInput.sak.person)
    }

    override fun behovForRevurdering(behandlingId: BehandlingId): List<VurderingsbehovMedPeriode> {
        // Ønsker ikke trigge revurdering pga dette informasjonskravet
        return emptyList()
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.UFØRE_SØKNAD

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): UføreSøknadInformasjonskrav {
            return UføreSøknadInformasjonskrav(repositoryProvider, gatewayProvider)
        }

        fun harEndringerIUføreSøknad(
            eksisterende: UføreSøknadGrunnlag?,
            uføreSøknad: UføreSøknad?
        ): Boolean {
            return if (uføreSøknad == null) {
                // Søknaden er ferdigbehandlet, men Kelvin skal ikke slette registeropplysninger
                // ettersom de kan blitt brukt for vurderingen av 11-18
                false
            } else {
                eksisterende?.uføreSøknad != uføreSøknad
            }
        }
    }
}
