package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.lookup.repository.RepositoryProvider

// Brukes via reflection/dynamisk oppslag av rammeverk, ikke fjern selv om den ser ubrukt ut
@Suppress("unused")
class AvbrytRevurderingVisningUtleder (
    private val behandlingRepository: BehandlingRepository,
) : StegGruppeVisningUtleder {
    constructor(repositoryProvider: RepositoryProvider): this(
        behandlingRepository = repositoryProvider.provide()
    )

    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val behandling = behandlingRepository.hent(behandlingId)
        return behandling.vurderingsbehov().any {
            it.type == Vurderingsbehov.REVURDERING_AVBRUTT
        }
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.AVBRYT_REVURDERING
    }
}