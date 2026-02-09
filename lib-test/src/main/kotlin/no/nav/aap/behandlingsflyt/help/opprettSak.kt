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

fun opprettSak(connection: DBConnection, periode: Periode): Sak {
    return opprettSak(connection, ident(), periode)
}

fun opprettSak(connection: DBConnection, ident: Ident, periode: Periode): Sak {
    return PersonOgSakService(
        FakePdlGateway,
        FakeApiInternGateway.konstruer(),
        PersonRepositoryImpl(connection),
        SakRepositoryImpl(connection)
    ).finnEllerOpprett(ident, periode)
}

fun opprettInMemorySak(ident: Ident, periode: Periode): Sak {
    return PersonOgSakService(
        FakePdlGateway,
        FakeApiInternGateway.konstruer(),
        InMemoryPersonRepository,
        InMemorySakRepository
    ).finnEllerOpprett(ident, periode)
}

fun opprettInMemorySak(periode: Periode): Sak {
    return opprettInMemorySak(ident(), periode)
}