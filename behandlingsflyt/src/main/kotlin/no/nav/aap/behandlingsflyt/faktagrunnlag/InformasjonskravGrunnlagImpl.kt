package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection

class InformasjonskravGrunnlagImpl(private val connection: DBConnection) : InformasjonskravGrunnlag {

    override fun oppdaterFaktagrunnlagForKravliste(
        kravliste: List<Informasjonskravkonstruktør>,
        kontekst: FlytKontekstMedPerioder
    ): List<Informasjonskravkonstruktør> {
        return kravliste
            .filter { kravtype -> kravtype.erRelevant(kontekst) }
            .filter { kravtype -> kravtype.konstruer(connection).oppdater(kontekst) == Informasjonskrav.Endret.ENDRET }
    }
}
