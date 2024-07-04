package no.nav.aap.behandlingsflyt.faktagrunnlag.register.Institusjonsopphold

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.Institusjonsopphold.adapter.InstitusjonsoppholdGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class InstitusjonsoppholdService private constructor(
    private val sakService: SakService,
    private val institusjonsoppholdRepository: InstitusjonsoppholdRepository,
    private val InstitusjonsoppholdRegisterGateway: InstitusjonsoppholdGateway
) : Informasjonskrav {

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val behandlingId = kontekst.behandlingId
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        val sak = sakService.hent(kontekst.sakId)

        val institusjonsopphold = InstitusjonsoppholdRegisterGateway.innhent(sak.person)

        institusjonsoppholdRepository.lagreOpphold(behandlingId, institusjonsopphold)

        return eksisterendeGrunnlag?.opphold == institusjonsopphold
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