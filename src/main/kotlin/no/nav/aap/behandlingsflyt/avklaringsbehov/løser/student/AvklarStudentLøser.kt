
package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.student

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class AvklarStudentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarStudentLøsning> {

    private val behandlingRepository = behandlingRepository(connection)
    private val studentRepository = StudentRepository(connection)

    override fun løs(kontekst: FlytKontekst, løsning: AvklarStudentLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

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