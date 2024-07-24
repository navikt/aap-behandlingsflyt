package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.adapter.UføreGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.flyt.FlytKontekst

class UføreService(
    private val sakService: SakService,
    private val uføreRepository: UføreRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val uføreRegisterGateway: UføreRegisterGateway
) : Informasjonskrav {
    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val sak = sakService.hent(kontekst.sakId)
        val fødselsdato =
            requireNotNull(personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)?.personopplysning?.fødselsdato)
        //val uføregrad = uføreRegisterGateway.innhent(sak.person, fødselsdato)
        val uføregrad = Uføre(Prosent(100))

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

        return nyeData == gamleData
    }

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): UføreService {
            return UføreService(
                SakService(connection),
                UføreRepository(connection),
                PersonopplysningRepository(connection),
                UføreGateway
            )
        }
    }
}