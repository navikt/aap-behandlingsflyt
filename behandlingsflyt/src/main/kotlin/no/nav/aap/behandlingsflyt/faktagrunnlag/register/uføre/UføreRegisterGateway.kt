package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import java.time.LocalDate

interface UføreRegisterGateway {
    fun innhent(
        person: Person,
        fom: LocalDate
    ): Uføre
}