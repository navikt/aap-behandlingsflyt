package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection

interface Informasjonskravkonstruktør {
    fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean
    fun konstruer(connection: DBConnection): Informasjonskrav
}
