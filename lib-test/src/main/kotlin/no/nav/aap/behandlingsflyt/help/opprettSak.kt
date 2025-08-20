package no.nav.aap.behandlingsflyt.help

import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode

fun opprettSak(connection: DBConnection, periode: Periode): Sak {
    return PersonOgSakService(
        FakePdlGateway,
        PersonRepositoryImpl(connection),
        SakRepositoryImpl(connection)
    ).finnEllerOpprett(ident(),periode)
}