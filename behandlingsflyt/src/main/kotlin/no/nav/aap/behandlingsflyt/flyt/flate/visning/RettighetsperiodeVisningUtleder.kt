package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider

@Suppress("unused") // reflection
class RettighetsperiodeVisningUtleder(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val rettighetsperiodeRepository: VurderRettighetsperiodeRepository
) : StegGruppeVisningUtleder {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        rettighetsperiodeRepository = repositoryProvider.provide()
    )
    override fun skalVises(behandlingId: BehandlingId): Boolean {
        if (rettighetsperiodeRepository.hentVurdering(behandlingId) != null) {
            return true
        }

        val hentAvklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        return hentAvklaringsbehovene
            .hentBehovForDefinisjon(Definisjon.VURDER_RETTIGHETSPERIODE)?.erIkkeAvbrutt() == true
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.RETTIGHETSPERIODE
    }
}