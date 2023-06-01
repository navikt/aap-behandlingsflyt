package no.nav.aap.domene.fagsak

import no.nav.aap.domene.person.Person
import no.nav.aap.domene.typer.Periode
import java.util.Locale

class Fagsak(val id: Long, val person: Person, val periode: Periode, private var status: Status = Status.OPPRETTET) {

    val saksnummer = id.toString(36)
        .uppercase(Locale.getDefault())
        .replace("O", "o")
        .replace("I", "i")

}
