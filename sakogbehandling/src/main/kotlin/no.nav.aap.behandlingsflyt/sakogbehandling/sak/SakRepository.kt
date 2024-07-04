package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.verdityper.sakogbehandling.SakId

interface SakRepository {

    fun finnEllerOpprett(person: Person, periode: Periode): Sak

    fun finnSakerFor(person: Person): List<Sak>

    fun finnAlle(): List<Sak>

    fun hent(sakId: SakId): Sak

    fun hent(saksnummer: Saksnummer): Sak

    fun finnBarn(saksnummer: Saksnummer): List<Ident>

}