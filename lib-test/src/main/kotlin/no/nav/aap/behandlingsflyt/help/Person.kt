package no.nav.aap.behandlingsflyt.help

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import java.util.Random
import java.util.UUID

private val random = Random()

private val randomIdent = generateSequence { random.nextInt(0, 10).toString() }
    .windowed(11, 11)
    .map { it.joinToString(separator = "") }
    .map { Ident(it) }
    .iterator()
    .iterator()

fun ident() = randomIdent.next()

fun person(
    identer: List<Ident>? = null,
) = Person(
    id = PersonId(random.nextLong()),
    referanse = UUID.randomUUID(),
    identer = identer ?: listOf(ident()),
)