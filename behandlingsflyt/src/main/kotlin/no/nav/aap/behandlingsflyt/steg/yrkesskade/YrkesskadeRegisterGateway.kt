package no.nav.aap.behandlingsflyt.steg.yrkesskade

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.personopplysninger.Fødselsdato
import no.nav.aap.yrkesskade.Yrkesskade

interface YrkesskadeRegisterGateway : Gateway {
    fun innhent(person: Person, fødselsdato: Fødselsdato): List<Yrkesskade>
}