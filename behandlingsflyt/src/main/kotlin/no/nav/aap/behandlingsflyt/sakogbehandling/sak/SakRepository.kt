package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDate

interface SakRepository : Repository {

    @Deprecated("Sluttdato for rettighetesperiode er alltid Tid.MAKS for nye/migrerte saker. Send kun med søknadsdato, med mindre du tester koden din for ikke-migrerte saker.")
    fun finnEllerOpprett(person: Person, periode: Periode): Sak {
        return finnEllerOpprett(person, periode.fom)
    }

    fun finnEllerOpprett(person: Person, søknadsdato: LocalDate): Sak

    fun finnSakerFor(person: Person): List<Sak>

    fun finnAlleSakIder(): List<SakId>

    fun finnSiste(antall: Int): List<Sak>

    fun hent(sakId: SakId): Sak

    fun hent(saksnummer: Saksnummer): Sak

    fun hentHvisFinnes(saksnummer: Saksnummer): Sak?

    fun finnPersonId(sakId: SakId): PersonId

    fun oppdaterRettighetsperiode(sakId: SakId, periode: Periode)

    fun oppdaterSakStatus(sakId: SakId, status: Status)

    fun finnSakerMedFritakMeldeplikt(): List<SakId>

    fun finnSakerMedInstitusjonsOpphold(): List<Sak>

    fun finnSakerMedAvsluttedeBehandlingerUtenRiktigSluttdatoPåRettighetsperiode(): List<Sak>

    fun finnSakerMedBarnetillegg(påDato: LocalDate): List<SakId>
}