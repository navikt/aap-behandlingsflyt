package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.lookup.repository.RepositoryProvider

class SøknadService private constructor(
    private val mottaDokumentService: MottaDokumentService,
    private val studentRepository: StudentRepository,
    private val barnRepository: BarnRepository,
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository
) : Informasjonskrav {

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.SØKNAD

        override fun konstruer(repositoryProvider: RepositoryProvider): SøknadService {
            val medlemskapArbeidInntektRepository = repositoryProvider.provide<MedlemskapArbeidInntektRepository>()
            return SøknadService(
                MottaDokumentService(repositoryProvider),
                repositoryProvider.provide<StudentRepository>(),
                repositoryProvider.provide(),
                medlemskapArbeidInntektRepository
            )
        }
    }

    override val navn = Companion.navn

    override fun erRelevant(kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering()
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val ubehandletSøknader = mottaDokumentService.søknaderSomIkkeHarBlittBehandlet(kontekst.sakId)
        if (ubehandletSøknader.isEmpty()) {
            return IKKE_ENDRET
        }

        val behandlingId = kontekst.behandlingId

        for (ubehandletSøknad in ubehandletSøknader) {
            studentRepository.lagre(
                behandlingId = behandlingId,
                if (ubehandletSøknad.studentData == null) null else
                    OppgittStudent(
                        erStudentStatus = ubehandletSøknad.studentData.erStudent,
                        skalGjenopptaStudieStatus = ubehandletSøknad.studentData.skalGjenopptaStudie
                    )
            )

            if (ubehandletSøknad.oppgitteBarn != null) {
                barnRepository.lagreOppgitteBarn(kontekst.behandlingId, ubehandletSøknad.oppgitteBarn)
            }

            if (ubehandletSøknad.utenlandsOppholdData != null) {
                medlemskapArbeidInntektRepository.lagreOppgittUtenlandsOppplysninger(
                    kontekst.behandlingId,
                    ubehandletSøknad.journalpostId,
                    ubehandletSøknad.utenlandsOppholdData
                )
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
