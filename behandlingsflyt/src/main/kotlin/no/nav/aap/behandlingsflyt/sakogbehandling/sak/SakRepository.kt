package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.type.Periode

interface SakRepository {

    fun finnEllerOpprett(person: Person, periode: Periode): Sak

    fun finnSakerFor(person: Person): List<Sak>

    fun finnAlle(): List<Sak>

    fun hent(sakId: SakId): Sak

    fun hent(saksnummer: Saksnummer): Sak

    fun finnSøker(saksnummer: Saksnummer): Person

    fun finnSøker(sakId: SakId): Person
}