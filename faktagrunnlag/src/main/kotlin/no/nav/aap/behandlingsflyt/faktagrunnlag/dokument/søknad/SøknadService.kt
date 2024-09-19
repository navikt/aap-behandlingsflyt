package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekst

class SøknadService private constructor(
    private val mottaDokumentService: MottaDokumentService,
    private val studentRepository: StudentRepository,
    private val barnRepository: BarnRepository
) : Informasjonskrav {

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): SøknadService {
            return SøknadService(
                MottaDokumentService(
                    MottattDokumentRepository(connection)
                ),
                StudentRepository(connection),
                BarnRepository(connection)
            )
        }
    }

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val ubehandletSøknader = mottaDokumentService.søknaderSomIkkeHarBlittBehandlet(kontekst.sakId)
        if (ubehandletSøknader.isEmpty()) {
            return true
        }

        val behandlingId = kontekst.behandlingId

        for (ubehandletSøknad in ubehandletSøknader) {
            studentRepository.lagre(
                behandlingId = behandlingId,
                OppgittStudent(
                    erStudentStatus = ubehandletSøknad.erStudent,
                    skalGjenopptaStudieStatus = ubehandletSøknad.skalGjenopptaStudie
                )
            )
            if (ubehandletSøknad.oppgitteBarn != null) {
                barnRepository.lagreOppgitteBarn(kontekst.behandlingId, ubehandletSøknad.oppgitteBarn)
            }

            mottaDokumentService.knyttTilBehandling(
                sakId = kontekst.sakId,
                behandlingId = kontekst.behandlingId,
                journalpostId = ubehandletSøknad.journalpostId
            )
        }

        return false // Antar her at alle nye søknader gir en endring vi må ta hensyn til
    }
}
