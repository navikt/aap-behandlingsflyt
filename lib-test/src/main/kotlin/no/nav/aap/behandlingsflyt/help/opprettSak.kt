package no.nav.aap.behandlingsflyt.help

import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeApiInternGateway
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

fun opprettSak(connection: DBConnection, søknadsdato: LocalDate): Sak {
    return opprettSak(connection, ident(), søknadsdato)
}

fun opprettSak(connection: DBConnection, ident: Ident, søknadsdato: LocalDate): Sak {
    return PersonOgSakService(
        FakePdlGateway,
        FakeApiInternGateway.konstruer(),
        PersonRepositoryImpl(connection),
        SakRepositoryImpl(connection)
    ).finnEllerOpprett(ident, søknadsdato)
}

fun opprettInMemorySak(ident: Ident, søknadsdato: LocalDate): Sak {
    return PersonOgSakService(
        FakePdlGateway,
        FakeApiInternGateway.konstruer(),
        InMemoryPersonRepository,
        InMemorySakRepository
    ).finnEllerOpprett(ident, søknadsdato)
}

fun opprettInMemorySak(søknadsdato: LocalDate): Sak {
    return opprettInMemorySak(ident(), søknadsdato)
}

@Deprecated("Sluttdato for rettighetesperiode er alltid Tid.MAKS for nye/migrerte saker. Send kun med søknadsdato, med mindre du tester koden din for ikke-migrerte saker.")
fun opprettSak(connection: DBConnection, periode: Periode): Sak {
    @Suppress("DEPRECATION")
    return opprettSak(connection, ident(), periode)
}

@Deprecated("Sluttdato for rettighetesperiode er alltid Tid.MAKS for nye/migrerte saker. Send kun med søknadsdato, med mindre du tester koden din for ikke-migrerte saker.")
fun opprettSak(connection: DBConnection, ident: Ident, periode: Periode): Sak {
    @Suppress("DEPRECATION")
    return PersonOgSakService(
        FakePdlGateway,
        FakeApiInternGateway.konstruer(),
        PersonRepositoryImpl(connection),
        SakRepositoryImpl(connection)
    ).finnEllerOpprett(ident, periode)
}

@Deprecated("Sluttdato for rettighetesperiode er alltid Tid.MAKS for nye/migrerte saker. Send kun med søknadsdato, med mindre du tester koden din for ikke-migrerte saker.")
fun opprettInMemorySak(ident: Ident, periode: Periode): Sak {
    @Suppress("DEPRECATION")
    return PersonOgSakService(
        FakePdlGateway,
        FakeApiInternGateway.konstruer(),
        InMemoryPersonRepository,
        InMemorySakRepository
    ).finnEllerOpprett(ident, periode)
}

@Deprecated("Sluttdato for rettighetesperiode er alltid Tid.MAKS for nye/migrerte saker. Send kun med søknadsdato, med mindre du tester koden din for ikke-migrerte saker.")
fun opprettInMemorySak(periode: Periode): Sak {
    @Suppress("DEPRECATION")
    return opprettInMemorySak(ident(), periode)
}