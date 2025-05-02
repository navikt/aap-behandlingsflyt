package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryRegistry

@Suppress("unused")
class BarnetilleggVisningUtleder(connection: DBConnection) : StegGruppeVisningUtleder {

    private val barnRepository = RepositoryRegistry.provider(connection).provide<BarnRepository>()

    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val harRegsiterBarn =
            barnRepository.hentHvisEksisterer(behandlingId)?.registerbarn?.identer?.isNotEmpty() == true
        if (harRegsiterBarn) {
            return true
        }
        return barnRepository.hentHvisEksisterer(behandlingId)?.oppgitteBarn?.identer?.isNotEmpty() == true
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.BARNETILLEGG
    }
}