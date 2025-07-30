package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.gateway.Gateway

interface InstitusjonsoppholdGateway : Gateway {
    fun innhent(person: Person): List<Institusjonsopphold>
}