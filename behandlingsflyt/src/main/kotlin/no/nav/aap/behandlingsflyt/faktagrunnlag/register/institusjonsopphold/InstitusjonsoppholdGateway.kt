package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person

interface InstitusjonsoppholdGateway {
    fun innhent(person: Person): List<Institusjonsopphold>
}