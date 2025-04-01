package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection

interface Informasjonskravkonstruktør {
    val navn: InformasjonskravNavn
    fun erRelevant(kontekst: FlytKontekstMedPerioder, oppdatert: InformasjonskravOppdatert?): Boolean
    fun konstruer(connection: DBConnection): Informasjonskrav
}
