package no.nav.aap.behandlingsflyt.faktagrunnlag.register.sykepengemaksdato

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.lookup.repository.Repository

interface SykepengeMaksdatoRepository : Repository {
    fun lagre(maksdatoHendelse: MaksdatoHendelse, person: Person)
    fun hentHvisEksisterer(person: Person): MaksdatoHendelse?
}