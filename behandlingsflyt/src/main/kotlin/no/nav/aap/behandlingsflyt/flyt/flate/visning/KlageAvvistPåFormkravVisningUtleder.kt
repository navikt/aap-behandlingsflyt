package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Avslått
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.DelvisOmgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Omgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.ÅrsakTilAvslag
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
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
        return avklagingsbehovene.hentBehovForDefinisjon(Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV) != null
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.KLAGE_AVVIST_PÅ_FORMKRAV
    }
}