package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType

class VurderStudentSteg private constructor(
    private val studentRepository: StudentRepository
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)

        if (studentGrunnlag != null && !studentGrunnlag.erKonsistent()) {
            return StegResultat(listOf(Definisjon.AVKLAR_STUDENT))
        }

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return VurderStudentSteg(StudentRepository(connection))
        }

        override fun type(): StegType {
            return StegType.AVKLAR_STUDENT
        }
    }
}
