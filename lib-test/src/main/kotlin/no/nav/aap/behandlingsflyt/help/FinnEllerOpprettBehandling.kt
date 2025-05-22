package no.nav.aap.behandlingsflyt.help

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

fun finnEllerOpprettBehandling(
    connection: DBConnection,
    sak: Sak,
    årsaker: List<Årsak> = listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
) = finnEllerOpprettBehandling(postgresRepositoryRegistry.provider(connection), sak.saksnummer, årsaker)

fun finnEllerOpprettBehandling(
    connection: DBConnection,
    sak: Sak,
    årsakTilBehandling: ÅrsakTilBehandling
) = finnEllerOpprettBehandling(
    postgresRepositoryRegistry.provider(connection),
    sak.saksnummer,
    listOf(Årsak(årsakTilBehandling))
)

fun finnEllerOpprettBehandling(
    repositoryProvider: RepositoryProvider,
    saksnummer: Saksnummer,
    årsaker: List<Årsak> = listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
) = SakOgBehandlingService(repositoryProvider)
    .finnEllerOpprettBehandling(saksnummer, årsaker)
