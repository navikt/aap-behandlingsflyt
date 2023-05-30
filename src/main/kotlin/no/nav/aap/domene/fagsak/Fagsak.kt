package no.nav.aap.domene.fagsak

import no.nav.aap.domene.person.Person
import no.nav.aap.domene.typer.Periode

class Fagsak(val id: Long, val person: Person, val periode: Periode, private var status: Status = Status.OPPRETTET) {
    // TODO
}
