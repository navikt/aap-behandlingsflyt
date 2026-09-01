package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetsType
import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class VirkningstidspunktService(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val behandlingRepository: BehandlingRepository,
    private val vedtakRepository: VedtakRepository,
    private val kvoteService: KvoteService,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        vedtakRepository = repositoryProvider.provide(),
        kvoteService = KvoteService(repositoryProvider, gatewayProvider),
    )

    fun finnVirkningstidspunkt(behandlingId: BehandlingId): LocalDate? {
        return finnVirkningstidspunkt(behandlingRepository.hent(behandlingId))
    }

    fun finnVirkningstidspunkt(behandling: Behandling): LocalDate? {
        return when {
            behandling.erYtelsesbehandling() && behandling.status().erVedtatt() ->
                vedtakRepository.hent(behandling.id)?.virkningstidspunkt

            behandling.erYtelsesbehandling() && behandling.status().erÅpen() ->
                utledVirkningstidspunkt(behandling.id)

            else ->
                null
        }
    }

    private fun utledVirkningstidspunkt(behandlingId: BehandlingId): LocalDate? {
        val vilkårsResultat = vilkårsresultatRepository.hent(behandlingId)
        return vurderRettighetsType(vilkårsResultat, kvoteService.gjeldendeKvoter()).segmenter().firstOrNull()?.periode?.fom
    }
}