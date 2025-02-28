package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderStudentSteg private constructor(
    private val studentRepository: StudentRepository
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        when (kontekst.vurdering.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)

                if (studentGrunnlag != null && !studentGrunnlag.erKonsistent()) {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_STUDENT)
                }
            }

            VurderingType.REVURDERING -> {
                val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)

                if (studentGrunnlag != null && !studentGrunnlag.erKonsistent()) {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_STUDENT)
                }
            }

            VurderingType.FORLENGELSE -> {
                // Do nothing
            }

            VurderingType.IKKE_RELEVANT -> {
                // Do nothing
            }
        }


        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return VurderStudentSteg(RepositoryProvider(connection).provide())
        }

        override fun type(): StegType {
            return StegType.AVKLAR_STUDENT
        }
    }
}
