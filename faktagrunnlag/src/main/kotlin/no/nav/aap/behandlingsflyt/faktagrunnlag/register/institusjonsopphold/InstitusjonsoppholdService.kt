package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.adapter.InstitusjonsoppholdGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class InstitusjonsoppholdService private constructor(
    private val sakService: SakService,
    private val institusjonsoppholdRepository: InstitusjonsoppholdRepository,
    private val institusjonsoppholdRegisterGateway: InstitusjonsoppholdGateway
) : Informasjonskrav {

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val behandlingId = kontekst.behandlingId
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        val sak = sakService.hent(kontekst.sakId)

        val rettighetsperiode = sak.rettighetsperiode

        val institusjonsopphold = institusjonsoppholdRegisterGateway.innhent(sak.person)
            .filter { it.periode().overlapper(rettighetsperiode) }

        institusjonsoppholdRepository.lagreOpphold(behandlingId, institusjonsopphold)

        return erUendret(eksisterendeGrunnlag, hentHvisEksisterer(behandlingId))
    }

    private fun erUendret(
        eksisterendeGrunnlag: InstitusjonsoppholdGrunnlag?,
        institusjonsopphold: InstitusjonsoppholdGrunnlag?
    ): Boolean {
        return eksisterendeGrunnlag?.opphold == institusjonsopphold?.opphold
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): InstitusjonsoppholdGrunnlag? {
        return institusjonsoppholdRepository.hentHvisEksisterer(behandlingId)
    }

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): InstitusjonsoppholdService {
            return InstitusjonsoppholdService(
                SakService(connection),
                InstitusjonsoppholdRepository(connection),
                InstitusjonsoppholdGateway
            )
        }
    }
}