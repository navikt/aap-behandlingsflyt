package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.DelvisOmgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Omgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Opprettholdes
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider

@Suppress("unused")
class OpprettholdelseVisningUtleder(
    private val klageresultatUtleder: KlageresultatUtleder,
) : StegGruppeVisningUtleder {
    constructor(repositoryProvider: RepositoryProvider) : this(
        klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
    )

    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val resultat = klageresultatUtleder.utledKlagebehandlingResultat(behandlingId)
        return when (resultat) {
            is Opprettholdes, is DelvisOmgjøres -> true
            else -> false
        }
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.OPPRETTHOLDELSE
    }
}