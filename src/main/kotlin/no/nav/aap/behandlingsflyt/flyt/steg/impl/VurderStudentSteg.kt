package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class VurderStudentSteg private constructor(
    private val studentRepository: StudentRepository
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)

        if (studentGrunnlag?.erKonsistent() != true) {
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
