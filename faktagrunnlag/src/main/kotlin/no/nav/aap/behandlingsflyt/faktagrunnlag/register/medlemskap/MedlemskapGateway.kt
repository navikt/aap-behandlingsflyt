package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person

interface MedlemskapGateway {
    fun innhent(person: Person): Medlemskap
}