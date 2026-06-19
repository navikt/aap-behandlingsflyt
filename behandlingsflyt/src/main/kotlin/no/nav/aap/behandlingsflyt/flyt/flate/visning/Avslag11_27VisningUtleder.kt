package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Repository
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.lookup.repository.RepositoryProvider

// Brukes via reflection/dynamisk oppslag av rammeverk, ikke fjern selv om den ser ubrukt ut
@Suppress("unused")
class Avslag11_27VisningUtleder (
    private val behandlingRepository: BehandlingRepository,
    private val avslag1127repository: Avslag11_27Repository,
) : StegGruppeVisningUtleder {
    constructor(repositoryProvider: RepositoryProvider): this(
        behandlingRepository = repositoryProvider.provide(),
        avslag1127repository = repositoryProvider.provide()
    )

    override fun skalVises(behandlingId: BehandlingId): Boolean {
    val grunnlag = avslag1127repository.hentHvisEksisterer(behandlingId)
    if (!grunnlag?.vurderinger.isNullOrEmpty()) return true

    return behandlingRepository
        .hent(behandlingId)
        .vurderingsbehov()
        .any { it.type == Vurderingsbehov.VURDER_AVSLAG_11_27 }
}

    override fun gruppe(): StegGruppe {
        return StegGruppe.AVSLAG_11_27
    }
}