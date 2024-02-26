package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import java.time.Year

interface InntektRegisterGateway {
    fun innhent(
        person: Person,
        år: Set<Year>
    ): Set<InntektPerÅr>
}