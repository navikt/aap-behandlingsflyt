package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSiste
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Duration

class UføreService(
    private val sakService: SakService,
    private val uføreRepository: UføreRepository,
    private val samordningUføreRepository: SamordningUføreRepository,
    private val uføreRegisterGateway: UføreRegisterGateway,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): this(
        sakService = SakService(repositoryProvider),
        uføreRepository = repositoryProvider.provide(),
        samordningUføreRepository = repositoryProvider.provide(),
        uføreRegisterGateway = gatewayProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    override val navn = Companion.navn

    override fun erRelevant(kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                oppdatert.ikkeKjørtSiste(Duration.ofHours(1)) &&
                !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)
        val uføregrader = uføreRegisterGateway.innhentMedHistorikk(sak.person, sak.rettighetsperiode.fom)

        val behandlingId = kontekst.behandlingId
        val eksisterendeGrunnlag = uføreRepository.hentHvisEksisterer(behandlingId)

        if (harEndringerUføre(eksisterendeGrunnlag, uføregrader)) {
            uføreRepository.lagre(behandlingId, uføregrader)
            return ENDRET
        }

        return IKKE_ENDRET
    }

    fun tidslinje(behandlingId: BehandlingId): Tidslinje<Prosent> {
        return samordningUføreRepository.hentHvisEksisterer(behandlingId)?.vurdering?.tilTidslinje() ?: Tidslinje.empty()
    }

    fun hentRegisterGrunnlagHvisEksisterer(behandlingId: BehandlingId): UføreGrunnlag? {
        return uføreRepository.hentHvisEksisterer(behandlingId)
    }

    fun hentVurderingGrunnlagHvisEksisterer(behandlingId: BehandlingId): SamordningUføreGrunnlag? {
        return samordningUføreRepository.hentHvisEksisterer(behandlingId)
    }

    private fun harEndringerUføre(
        eksisterende: UføreGrunnlag?,
        uføregrader: List<Uføre>
    ): Boolean {
        if (eksisterende == null && uføregrader.isEmpty()) {
            return false
        }
        return uføregrader != eksisterende?.vurderinger
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.UFØRE

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): UføreService {
            return UføreService(repositoryProvider, gatewayProvider)
        }
    }
}