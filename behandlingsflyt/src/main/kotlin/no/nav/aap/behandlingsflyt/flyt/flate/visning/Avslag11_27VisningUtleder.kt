package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.steg.samordning.Avslag11_27Repository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

// Brukes via reflection/dynamisk oppslag av rammeverk, ikke fjern selv om den ser ubrukt ut
@Suppress("unused")
class Avslag11_27VisningUtleder(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlingRepository: BehandlingRepository,
    private val avslag1127repository: Avslag11_27Repository,
) : StegGruppeVisningUtleder {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        avslag1127repository = repositoryProvider.provide()
    )

    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val grunnlag = avslag1127repository.hentHvisEksisterer(behandlingId)
        if (!grunnlag?.vurderinger.isNullOrEmpty()) return true

        val hentAvklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        return hentAvklaringsbehovene
            .hentBehovForDefinisjon(Definisjon.VURDER_AVSLAG_11_27)?.erIkkeAvbrutt() == true
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.AVSLAG_11_27
    }
}