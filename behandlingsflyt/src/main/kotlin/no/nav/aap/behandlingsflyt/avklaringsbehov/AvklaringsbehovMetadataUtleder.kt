package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.tidslinje.Tidslinje

interface AvklaringsbehovMetadataUtleder {
    fun nårVurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean>
    val stegType: StegType
}