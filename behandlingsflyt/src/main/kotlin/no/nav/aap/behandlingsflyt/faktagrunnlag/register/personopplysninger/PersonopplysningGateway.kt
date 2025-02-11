package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person

interface PersonopplysningGateway {
    fun innhent(person: Person): Personopplysning?
    fun innhentMedHistorikk(person: Person): PersonopplysningMedHistorikk?
}