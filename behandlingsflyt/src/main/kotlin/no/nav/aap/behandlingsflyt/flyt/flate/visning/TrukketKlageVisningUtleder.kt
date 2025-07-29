package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.lookup.repository.RepositoryProvider

// Denne ser ubrukt ut, men er ikke det pga reflection
@Suppress("unused")
class TrukketKlageVisningUtleder(
    private val behandlingRepository: BehandlingRepository,
) : StegGruppeVisningUtleder {

    constructor(repositoryProvider: RepositoryProvider): this(
        behandlingRepository = repositoryProvider.provide()
    )

    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val behandling = behandlingRepository.hent(behandlingId)
        return behandling.vurderingsbehov().any {
            it.type == Vurderingsbehov.KLAGE_TRUKKET
        }
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.TREKK_KLAGE
    }
}