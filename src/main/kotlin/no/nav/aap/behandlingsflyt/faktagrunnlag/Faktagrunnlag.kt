package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class Faktagrunnlag {

    fun oppdaterFaktagrunnlagForKravliste(kravliste: List<Grunnlag<*>>, kontekst: FlytKontekst): List<Grunnlag<*>> {
        // Hva gir dette leddet?
        return kravliste.filterNot { grunnlagstype -> grunnlagstype.oppdater(kontekst) }
    }
}
