
package no.nav.aap.behandlingsflyt.avklaringsbehov.student

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.grunnlag.student.db.InMemoryStudentRepository

class AvklarStudentLøser : AvklaringsbehovsLøser<AvklarStudentLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: AvklarStudentLøsning): LøsningsResultat {
        val behandling = BehandlingTjeneste.hent(kontekst.behandlingId)

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