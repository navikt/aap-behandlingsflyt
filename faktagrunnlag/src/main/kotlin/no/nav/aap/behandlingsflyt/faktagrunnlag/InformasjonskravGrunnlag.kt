package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class InformasjonskravGrunnlag(private val connection: DBConnection) {

    fun oppdaterFaktagrunnlagForKravliste(
        kravliste: List<Informasjonskravkonstruktør>,
        kontekst: FlytKontekstMedPerioder
    ): List<Informasjonskravkonstruktør> {
        // Hva gir dette leddet?
        return kravliste.filter { kravtype -> kravtype.erRelevant(kontekst) }.filterNot { kravtype -> kravtype.konstruer(connection).harIkkeGjortOppdateringNå(kontekst) }
    }
}
