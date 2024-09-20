package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter.YrkesskadeRegisterGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class SøknadService private constructor(
    private val mottaDokumentService: MottaDokumentService,
    private val studentRepository: StudentRepository,
    private val sakService: SakService,
    private val barnRepository: BarnRepository
) : Informasjonskrav {

    companion object : Informasjonskravkonstruktør {
        override fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
            // Skal alltid innhentes
            return true
        }
        override fun konstruer(connection: DBConnection): SøknadService {
            return SøknadService(
                MottaDokumentService(
                    MottattDokumentRepository(connection)
                ),
                StudentRepository(connection),
                SakService(connection),
                BarnRepository(connection)
            )
        }
    }

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekstMedPerioder): Boolean {
        val ubehandletSøknader = mottaDokumentService.søknaderSomIkkeHarBlittBehandlet(kontekst.sakId)
        if (ubehandletSøknader.isEmpty()) {
            return true
        }

        val behandlingId = kontekst.behandlingId
        val sak = sakService.hent(kontekst.sakId)

        for (ubehandletSøknad in ubehandletSøknader) {
            studentRepository.lagre(
                behandlingId = behandlingId,
                OppgittStudent(
                    erStudentStatus = ubehandletSøknad.erStudent,
                    skalGjenopptaStudieStatus = ubehandletSøknad.skalGjenopptaStudie
                )
            )
            //FIXME: Brukes i dev på grunn av manglende integrasjon mot yrkesskaderegisteret i dolly
            if (ubehandletSøknad.harYrkesskade) {
                YrkesskadeRegisterGateway.puttInnTestPerson(
                    sak.person.aktivIdent(),
                    sak.rettighetsperiode.fom.minusDays(60)
                )
            }
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
