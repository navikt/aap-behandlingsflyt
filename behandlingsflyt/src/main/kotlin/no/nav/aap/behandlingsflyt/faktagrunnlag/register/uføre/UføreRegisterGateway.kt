package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.lookup.gateway.Gateway
import java.time.LocalDate

interface UføreRegisterGateway : Gateway {
    fun innhent(
        person: Person,
        forDato: LocalDate
    ): List<Uføre>
}