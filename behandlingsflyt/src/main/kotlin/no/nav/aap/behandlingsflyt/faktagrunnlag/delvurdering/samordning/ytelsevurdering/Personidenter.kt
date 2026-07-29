package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository

/**
 * Finner alle kjente identer (inkludert historiske) for personen.
 *
 * Dersom personen ikke er kjent i AAP brukes kun den innsendte identen.
 */
internal fun PersonRepository.finnAlleIdenter(personident: String): Set<String> {
    val person = finn(Ident(personident))
    val kjenteIdenter = person?.identer()?.map { it.identifikator }.orEmpty()
    return (kjenteIdenter + personident).toSet()
}

