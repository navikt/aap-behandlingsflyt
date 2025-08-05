package no.nav.aap.behandlingsflyt.help

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

fun finnEllerOpprettBehandling(
    connection: DBConnection,
    sak: Sak,
    vurderingsbehov: List<VurderingsbehovMedPeriode> = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
    årsakTilOpprettelse: ÅrsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD
) = finnEllerOpprettBehandling(postgresRepositoryRegistry.provider(connection), sak.saksnummer, vurderingsbehov, årsakTilOpprettelse)

fun finnEllerOpprettBehandling(
    connection: DBConnection,
    sak: Sak,
    vurderingsbehov: Vurderingsbehov,
    årsakTilOpprettelse: ÅrsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD
) = finnEllerOpprettBehandling(
    postgresRepositoryRegistry.provider(connection),
    sak.saksnummer,
    listOf(VurderingsbehovMedPeriode(vurderingsbehov))
)

fun finnEllerOpprettBehandling(
    repositoryProvider: RepositoryProvider,
    saksnummer: Saksnummer,
    vurderingsbehov: List<VurderingsbehovMedPeriode> = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
    årsakTilOpprettelse: ÅrsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD
) = SakOgBehandlingService(repositoryProvider, FakeUnleash)
    .finnEllerOpprettBehandling(saksnummer, vurderingsbehov, årsakTilOpprettelse)
