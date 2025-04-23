package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDate

interface SakRepository : Repository {

    fun finnEllerOpprett(person: Person, periode: Periode, søknadsdato: LocalDate): Sak

    fun finnSakerFor(person: Person): List<Sak>

    fun finnAlle(): List<Sak>

    fun hent(sakId: SakId): Sak

    fun hent(saksnummer: Saksnummer): Sak

    fun finnSøker(saksnummer: Saksnummer): Person

    fun finnSøker(sakId: SakId): Person

    fun oppdaterRettighetsperiode(sakId: SakId, periode: Periode)

    fun oppdaterSakStatus(sakId: SakId, status: Status)
}