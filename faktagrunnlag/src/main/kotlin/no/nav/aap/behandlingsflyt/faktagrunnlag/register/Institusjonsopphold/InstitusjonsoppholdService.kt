package no.nav.aap.behandlingsflyt.faktagrunnlag.register.Institusjonsopphold

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagkonstruktør
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.tidslinje.Segment
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.Institusjonsopphold.adapter.InstitusjonsoppholdGateway

class InstitusjonsoppholdService private constructor(
    private val sakService: SakService,
    private val institusjonsoppholdRepository: InstitusjonsoppholdRepository,
    private val InstitusjonsoppholdRegisterGateway: InstitusjonsoppholdGateway
) : Grunnlag {

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val behandlingId = kontekst.behandlingId
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        val sak = sakService.hent(kontekst.sakId)

        val institusjonsopphold = InstitusjonsoppholdRegisterGateway.innhent(sak.person)

        institusjonsoppholdRepository.lagreOpphold(behandlingId, institusjonsopphold)

        return eksisterendeGrunnlag == institusjonsopphold
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): List<Segment<Institusjon>> {
        return institusjonsoppholdRepository.hent(behandlingId)
    }

    companion object : Grunnlagkonstruktør {
        override fun konstruer(connection: DBConnection): InstitusjonsoppholdService {
            return InstitusjonsoppholdService(
                SakService(connection),
                InstitusjonsoppholdRepository(connection),
                InstitusjonsoppholdGateway
            )
        }
    }
}