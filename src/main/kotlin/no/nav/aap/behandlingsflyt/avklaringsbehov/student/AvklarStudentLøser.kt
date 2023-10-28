
package no.nav.aap.behandlingsflyt.avklaringsbehov.student

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.db.InMemoryStudentRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class AvklarStudentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarStudentLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: AvklarStudentLøsning): LøsningsResultat {
        val behandling = BehandlingRepository(connection).hent(kontekst.behandlingId)

        InMemoryStudentRepository.lagre(
            behandlingId = behandling.id,
            studentvurdering = løsning?.studentvurdering,
        )

        return LøsningsResultat(
            begrunnelse = løsning.studentvurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_STUDENT
    }
}