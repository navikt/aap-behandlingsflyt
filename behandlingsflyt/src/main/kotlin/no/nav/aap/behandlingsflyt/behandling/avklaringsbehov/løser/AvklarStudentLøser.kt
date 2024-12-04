
package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection

class AvklarStudentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarStudentLøsning> {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val studentRepository = StudentRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarStudentLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        studentRepository.lagre(
            behandlingId = behandling.id,
            studentvurdering = løsning.studentvurdering,
        )

        return LøsningsResultat(
            begrunnelse = løsning.studentvurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_STUDENT
    }
}