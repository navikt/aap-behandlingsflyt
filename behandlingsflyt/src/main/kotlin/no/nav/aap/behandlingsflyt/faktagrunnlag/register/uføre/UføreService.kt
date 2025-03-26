package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class UføreService(
    private val sakService: SakService,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val uføreRepository: UføreRepository,
    private val uføreRegisterGateway: UføreRegisterGateway
) : Informasjonskrav {
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
        val sak = sakOgBehandlingService.hentSakFor(behandlingId)
        return uføreRepository.hentHvisEksisterer(behandlingId)
            ?.vurderinger
            .orEmpty()
            .map {
                Segment(
                    Periode(it.virkningstidspunkt, sak.rettighetsperiode.tom),
                    it.uføregrad
                )
            }
            .let(::Tidslinje)
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

        override fun konstruer(connection: DBConnection): UføreService {
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide<SakRepository>()
            return UføreService(
                sakService = SakService(sakRepository),
                sakOgBehandlingService = SakOgBehandlingService(
                    grunnlagKopierer = GrunnlagKopierer(connection),
                    sakRepository = repositoryProvider.provide(),
                    behandlingRepository = repositoryProvider.provide(),
                ),
                uføreRegisterGateway = GatewayProvider.provide<UføreRegisterGateway>(),
                uføreRepository = repositoryProvider.provide(),
            )
        }
    }
}