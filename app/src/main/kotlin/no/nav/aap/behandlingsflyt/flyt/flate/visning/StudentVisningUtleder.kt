package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.StegGruppe
import no.nav.aap.verdityper.sakogbehandling.BehandlingId


class StudentVisningUtleder(connection: DBConnection) : StegGruppeVisningUtleder {

    private val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)
    private val studentRepository = StudentRepository(connection)

    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId)
        if (studentGrunnlag?.studentvurdering != null) {
            return true
        }
        val hentAvklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        return hentAvklaringsbehovene
            .hentBehovForDefinisjon(Definisjon.AVKLAR_STUDENT)?.erIkkeAvbrutt() == true
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.STUDENT
    }
}