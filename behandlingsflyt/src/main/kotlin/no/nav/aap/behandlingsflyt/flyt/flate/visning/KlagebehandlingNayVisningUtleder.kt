package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetRepository
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider

@Suppress("unused")
class KlagebehandlingNayVisningUtleder(private val behandlendeEnhetRepository: BehandlendeEnhetRepository) :
    StegGruppeVisningUtleder {
    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlendeEnhetRepository = repositoryProvider.provide()
    )

    override fun skalVises(behandlingId: BehandlingId): Boolean {
        return behandlendeEnhetRepository.hentHvisEksisterer(behandlingId)?.vurdering?.skalBehandlesAvNay == true
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.KLAGEBEHANDLING_NAY
    }
}