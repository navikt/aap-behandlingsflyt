package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class EtAnnetStedVisningUtleder(connection: DBConnection) : StegGruppeVisningUtleder {

    private val institusjonsoppholdRepository = InstitusjonsoppholdRepository(connection)

    override fun skalVises(behandlingId: BehandlingId): Boolean {
        return !institusjonsoppholdRepository.hentHvisEksisterer(behandlingId)?.oppholdene?.opphold.isNullOrEmpty()
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.ET_ANNET_STED
    }
}