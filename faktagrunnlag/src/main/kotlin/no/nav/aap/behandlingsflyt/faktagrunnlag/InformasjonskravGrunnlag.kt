package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekst

class InformasjonskravGrunnlag(private val connection: DBConnection) {

    fun oppdaterFaktagrunnlagForKravliste(
        kravliste: List<Informasjonskravkonstruktør>,
        kontekst: FlytKontekst
    ): List<Informasjonskravkonstruktør> {
        // Hva gir dette leddet?
        return kravliste.filterNot { kravtype -> kravtype.konstruer(connection).harIkkeGjortOppdateringNå(kontekst) }
    }
}
