package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.datadeling.SakStatus.DatadelingBehandlingStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class SakstatusDatadelingService(
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val behandlingService: BehandlingService,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val trukketSøknadService: TrukketSøknadService,
    private val avbrytRevurderingService: AvbrytRevurderingService,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        repositoryProvider.provide(),
        repositoryProvider.provide(),
        BehandlingService(repositoryProvider, gatewayProvider),
        mottattDokumentRepository = repositoryProvider.provide(),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider),
    )

    fun utledSakstatus(referanse: BehandlingReferanse): SakStatus {
        val behandling = behandlingRepository.hent(referanse)
        val sak = sakRepository.hent(behandling.sakId)

        val datadelingBehandlingStatus = when {
            trukketSøknadService.søknadErTrukket(behandling.id) ||
                avbrytRevurderingService.revurderingErAvbrutt(behandling.id) -> DatadelingBehandlingStatus.FERDIGBEHANDLET

            else -> {
                val sisteYtelseBehandling =
                    requireNotNull(behandlingService.finnSisteGjeldendeEllerÅpneYtelsesbehandling(sak.id)) { "Fant ingen ytelsesbehandling for sak ${sak.id}" }

                when {
                    sisteYtelseBehandling.status()
                        .erÅpen() && sisteYtelseBehandling.vurderingsbehov()
                        .find { it.type == Vurderingsbehov.MOTTATT_SØKNAD } != null -> DatadelingBehandlingStatus.SOKNAD_UNDER_BEHANDLING

                    sisteYtelseBehandling.status().erÅpen() -> DatadelingBehandlingStatus.REVURDERING_UNDER_BEHANDLING

                    else -> DatadelingBehandlingStatus.FERDIGBEHANDLET
                }
            }
        }

        val søknadsdatoer = mottattDokumentRepository
            .hentDokumenterAvType(sak.id, InnsendingType.SØKNAD)
            .map { it.mottattTidspunkt.toLocalDate() }

        return SakStatus.fromKelvin(
            saksnummer = sak.saksnummer.toString(),
            datadelingBehandlingStatus = datadelingBehandlingStatus,
            periode = sak.rettighetsperiode,
            søknadsdatoer = søknadsdatoer
        )
    }
}