package no.nav.aap.behandlingsflyt.help

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

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
) = SakOgBehandlingService(repositoryProvider, gatewayProvider)
    .finnEllerOpprettBehandling(saksnummer, VurderingsbehovOgÅrsak(vurderingsbehov, årsakTilOpprettelse))
