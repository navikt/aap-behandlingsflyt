package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.gateway.Gateway

interface PersonopplysningGateway : Gateway {
    fun innhent(person: Person): Personopplysning?
    fun innhentMedHistorikk(person: Person): PersonopplysningMedHistorikk?
}