package no.nav.aap.behandlingsflyt.help.inmemory

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import java.time.LocalDate
import java.util.Random
import java.util.UUID


fun genererPerson(fødselsdato: LocalDate = LocalDate.now().minusYears(23)): Person = Person(
    PersonId(Random(1235123).nextLong()), UUID.randomUUID(), listOf(genererIdent(fødselsdato))
)