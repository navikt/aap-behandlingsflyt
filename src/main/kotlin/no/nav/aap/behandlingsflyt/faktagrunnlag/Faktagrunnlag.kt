package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class Faktagrunnlag(private val connection: DBConnection) {

    fun oppdaterFaktagrunnlagForKravliste(kravliste: List<Grunnlagkonstruktør>, kontekst: FlytKontekst): List<Grunnlagkonstruktør> {
        // Hva gir dette leddet?
        return kravliste.filterNot { grunnlagstype -> grunnlagstype.konstruer(connection).oppdater(kontekst) }
    }
}
