package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Repository

interface SakRepository : Repository {

    fun finnEllerOpprett(person: Person, periode: Periode): Sak

    fun finnSakerFor(person: Person): List<Sak>

    fun finnAlleSakIder(): List<SakId>

    fun finnSiste(antall: Int): List<Sak>

    fun hent(sakId: SakId): Sak

    fun hent(saksnummer: Saksnummer): Sak

    fun hentHvisFinnes(saksnummer: Saksnummer): Sak?

    fun finnPersonId(sakId: SakId): PersonId

    fun oppdaterRettighetsperiode(sakId: SakId, periode: Periode)

    fun oppdaterSakStatus(sakId: SakId, status: Status)
}