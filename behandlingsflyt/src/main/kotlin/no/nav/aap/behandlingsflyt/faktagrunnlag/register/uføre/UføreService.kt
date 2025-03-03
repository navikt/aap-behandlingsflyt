package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.adapter.UføreGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider

class UføreService(
    private val sakService: SakService,
    private val uføreRepository: UføreRepository,
    private val uføreRegisterGateway: UføreRegisterGateway
) : Informasjonskrav {
    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)
        val uføregrad = uføreRegisterGateway.innhent(sak.person, sak.rettighetsperiode.fom)

        val behandlingId = kontekst.behandlingId
        val gamleData = uføreRepository.hentHvisEksisterer(behandlingId)

        if (uføregrad.uføregrad.prosentverdi() != 0) {
            uføreRepository.lagre(
                behandlingId,
                uføregrad
            )
        } else if (gamleData != null) {
            uføreRepository.lagre(behandlingId, Uføre(Prosent(0)))
        }

        val nyeData = uføreRepository.hentHvisEksisterer(behandlingId)

        return if (nyeData == gamleData) IKKE_ENDRET else ENDRET
    }

    companion object : Informasjonskravkonstruktør {

        override fun konstruer(connection: DBConnection): UføreService {
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide<SakRepository>()
            return UføreService(
                SakService(sakRepository),
                repositoryProvider.provide(),
                UføreGateway
            )
        }
    }
}