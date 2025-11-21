package no.nav.aap.behandlingsflyt.help

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

fun finnEllerOpprettBehandling(
    connection: DBConnection,
    sak: Sak,
    vurderingsbehov: List<VurderingsbehovMedPeriode> = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
    årsakTilOpprettelse: ÅrsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
    gatewayProvider: GatewayProvider = createGatewayProvider {
        register<FakeUnleash>()
    }
): Behandling = finnEllerOpprettBehandling(
    repositoryProvider = postgresRepositoryRegistry.provider(connection),
    gatewayProvider = gatewayProvider,
    saksnummer = sak.saksnummer,
    vurderingsbehov = vurderingsbehov,
    årsakTilOpprettelse = årsakTilOpprettelse,
)

fun finnEllerOpprettBehandling(
    connection: DBConnection,
    sak: Sak,
    vurderingsbehov: Vurderingsbehov,
    årsakTilOpprettelse: ÅrsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
    gatewayProvider: GatewayProvider = createGatewayProvider {
        register<FakeUnleash>()
    }
): Behandling = finnEllerOpprettBehandling(
    connection = connection,
    sak = sak,
    vurderingsbehov = listOf(VurderingsbehovMedPeriode(vurderingsbehov)),
    årsakTilOpprettelse = årsakTilOpprettelse,
    gatewayProvider = gatewayProvider,
)

fun finnEllerOpprettBehandling(
    repositoryProvider: RepositoryProvider,
    gatewayProvider: GatewayProvider,
    saksnummer: Saksnummer,
    vurderingsbehov: List<VurderingsbehovMedPeriode> = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
    årsakTilOpprettelse: ÅrsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
): Behandling {
    val sak = repositoryProvider.provide<SakRepository>().hent(saksnummer)
    return SakOgBehandlingService(repositoryProvider, gatewayProvider)
        .finnEllerOpprettOrdinærBehandling(sak.id, VurderingsbehovOgÅrsak(vurderingsbehov, årsakTilOpprettelse))
}

fun sak(connection: DBConnection, periode: Periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))): Sak {
    return sak(postgresRepositoryRegistry.provider(connection), periode)
}

fun sak(
    repositoryProvider: RepositoryProvider,
    periode: Periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
): Sak {
    return PersonOgSakService(
        FakePdlGateway,
        repositoryProvider.provide(),
        repositoryProvider.provide()
    ).finnEllerOpprett(ident(), periode)
}