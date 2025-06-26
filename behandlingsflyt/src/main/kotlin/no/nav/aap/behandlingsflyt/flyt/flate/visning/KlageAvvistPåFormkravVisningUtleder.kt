package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider

@Suppress("unused")
class KlageAvvistPåFormkravVisningUtleder(
    private val avklaringsbehov: AvklaringsbehovRepository,
) : StegGruppeVisningUtleder {
    constructor(repositoryProvider: RepositoryProvider) : this(
        repositoryProvider.provide(),
    )

    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val avklagingsbehovene = avklaringsbehov.hentAvklaringsbehovene(behandlingId)
        return avklagingsbehovene.alle().any { it.skalLøsesIStegGruppe(gruppe()) }
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.KLAGE_AVVIST_PÅ_FORMKRAV
    }
}