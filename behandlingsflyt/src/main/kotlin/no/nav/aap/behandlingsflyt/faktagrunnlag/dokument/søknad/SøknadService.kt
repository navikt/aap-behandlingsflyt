package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

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
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide(SakRepository::class)
            return SøknadService(
                MottaDokumentService(
                    MottattDokumentRepositoryImpl(connection)
                ),
                StudentRepository(connection),
                SakService(sakRepository),
                BarnRepository(connection)
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val ubehandletSøknader = mottaDokumentService.søknaderSomIkkeHarBlittBehandlet(kontekst.sakId)
        if (ubehandletSøknader.isEmpty()) {
            return IKKE_ENDRET
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

            if (ubehandletSøknad.oppgitteBarn != null) {
                barnRepository.lagreOppgitteBarn(kontekst.behandlingId, ubehandletSøknad.oppgitteBarn)
            }

            mottaDokumentService.knyttTilBehandling(
                sakId = kontekst.sakId,
                behandlingId = kontekst.behandlingId,
                referanse = InnsendingReferanse(ubehandletSøknad.journalpostId)
            )
        }

        return ENDRET // Antar her at alle nye søknader gir en endring vi må ta hensyn til
    }
}
