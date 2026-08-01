package no.nav.aap.behandlingsflyt.steg.institusjon

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.misc.institusjonsopphold.Institusjonsopphold

interface InstitusjonsoppholdGateway : Gateway {
    fun innhent(person: Person): List<Institusjonsopphold>
    fun hentDataForHendelse(oppholdId: Long): Institusjonsopphold
}