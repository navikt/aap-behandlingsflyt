package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person

interface UføreRegisterGateway {
    fun innhent(
        person: Person,
        Fødselsdato: Fødselsdato
    ): Uføre
}