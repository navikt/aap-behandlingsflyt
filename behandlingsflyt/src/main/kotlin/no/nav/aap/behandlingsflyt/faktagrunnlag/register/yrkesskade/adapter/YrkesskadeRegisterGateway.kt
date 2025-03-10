package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.lookup.gateway.Gateway

interface YrkesskadeRegisterGateway : Gateway {
    fun innhent(person: Person, fødselsdato: Fødselsdato, oppgittYrkesskade: YrkesskadeModell?): List<Yrkesskade>
}