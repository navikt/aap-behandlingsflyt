package no.nav.aap.behandlingsflyt.help

import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeApiInternGateway
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryservice.InMemoryBehandlingService
import no.nav.aap.komponenter.dbconnect.DBConnection
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

fun opprettInMemorySak(søknadsdato: LocalDate = LocalDate.now(), ident: Ident = ident()): Sak {
    return PersonOgSakService(
        FakePdlGateway,
        FakeApiInternGateway.konstruer(),
        InMemoryPersonRepository,
        InMemorySakRepository
    ).finnEllerOpprett(ident, søknadsdato)
}

fun opprettInMemorySakOgBehandling(
    søknadsdato: LocalDate = LocalDate.now(),
    vurderingsbehov: List<VurderingsbehovMedPeriode> = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
    årsakTilOpprettelse: ÅrsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
    ident: Ident = ident(),
): Pair<Sak, Behandling> {
    val sak = opprettInMemorySak(søknadsdato, ident)
    val behandling = InMemoryBehandlingService.finnEllerOpprettOrdinærBehandling(
        sak.id,
        VurderingsbehovOgÅrsak(vurderingsbehov, årsakTilOpprettelse)
    )
    return sak to behandling
}