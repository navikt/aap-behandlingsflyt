package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class RettighetsperiodeVisningUtleder(connection: DBConnection) : StegGruppeVisningUtleder {

    private val repositoryProvider = RepositoryProvider(connection)
    private val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()

    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val hentAvklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        return hentAvklaringsbehovene
            .hentBehovForDefinisjon(Definisjon.VURDER_RETTIGHETSPERIODE)?.erIkkeAvbrutt() == true
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.RETTIGHETSPERIODE
    }
}