package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.DelvisOmgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Omgjøres
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider

@Suppress("unused")
class OmgjøringVisningUtleder(
    private val klageresultatUtleder: KlageresultatUtleder,
) : StegGruppeVisningUtleder {
    constructor(repositoryProvider: RepositoryProvider) : this(
        klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
    )

    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val resultat = klageresultatUtleder.utledKlagebehandlingResultat(behandlingId)
        return when (resultat) {
            is Omgjøres, is DelvisOmgjøres -> true
            else -> false
        }
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.OMGJØRING
    }
}