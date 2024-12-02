package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder

class InformasjonskravGrunnlagImpl(private val connection: DBConnection) : InformasjonskravGrunnlag {

    override fun oppdaterFaktagrunnlagForKravliste(
        kravliste: List<Informasjonskravkonstruktør>,
        kontekst: FlytKontekstMedPerioder
    ): List<Informasjonskravkonstruktør> {
        // Hva gir dette leddet?
        return kravliste
            .filter { kravtype -> kravtype.erRelevant(kontekst) }
            .filter { kravtype -> kravtype.konstruer(connection).oppdater(kontekst) == Informasjonskrav.Endret.ENDRET }
    }
}
