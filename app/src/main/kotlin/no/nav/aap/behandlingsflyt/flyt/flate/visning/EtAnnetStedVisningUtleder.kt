package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.Institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.verdityper.flyt.StegGruppe
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class EtAnnetStedVisningUtleder(connection: DBConnection) : StegGruppeVisningUtleder {

    private val institusjonsoppholdRepository = InstitusjonsoppholdRepository(connection)

    override fun skalVises(behandlingId: BehandlingId): Boolean {
        return institusjonsoppholdRepository.hentHvisEksisterer(behandlingId) != null
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.STUDENT
    }

}