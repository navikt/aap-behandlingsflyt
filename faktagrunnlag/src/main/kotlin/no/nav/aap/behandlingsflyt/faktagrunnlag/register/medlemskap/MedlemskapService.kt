package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter.MedlemskapGateway
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class MedlemskapService private constructor(
    private val medlemskapGateway: MedlemskapGateway,
    private val sakService: SakService,
    private val medlemskapRepository: MedlemskapRepository,
) : Informasjonskrav {
    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val behandlingId = kontekst.behandlingId
        val sak = sakService.hent(kontekst.sakId)
        val medlemskapPerioder = medlemskapGateway.innhent(sak.person)
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        medlemskapRepository.lagreUnntakMedlemskap(kontekst.behandlingId, medlemskapPerioder)

        return erUendret(eksisterendeGrunnlag, hentHvisEksisterer(behandlingId))
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapUnntakGrunnlag? {
        return medlemskapRepository.hentHvisEksisterer(behandlingId)
    }

    private fun erUendret(
        eksisterendeGrunnlag: MedlemskapUnntakGrunnlag?,
        nyttGrunnlag: MedlemskapUnntakGrunnlag?
    ): Boolean {
        return eksisterendeGrunnlag?.unntak == nyttGrunnlag?.unntak
    }

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): MedlemskapService {
            return MedlemskapService(
                MedlemskapGateway(),
                SakService(connection),
                MedlemskapRepository(connection)
            )
        }
    }
}