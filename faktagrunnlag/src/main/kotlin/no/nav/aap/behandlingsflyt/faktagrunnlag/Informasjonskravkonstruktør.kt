package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

interface Informasjonskravkonstruktør {
    fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean
    fun konstruer(connection: DBConnection): Informasjonskrav
}
