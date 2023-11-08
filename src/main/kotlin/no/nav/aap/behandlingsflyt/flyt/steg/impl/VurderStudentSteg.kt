package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat

class VurderStudentSteg(
    private val studentRepository: StudentRepository
) : BehandlingSteg {

    override fun utfør(input: StegInput): StegResultat {
        val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId = input.kontekst.behandlingId)

        if (studentGrunnlag?.erKonsistent() != true) {
            return StegResultat(listOf(Definisjon.AVKLAR_STUDENT))
        }

        return StegResultat()
    }
}
