package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetsType
import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class VirkningstidspunktUtleder(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val kvoteService: KvoteService,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        kvoteService = KvoteService(repositoryProvider, gatewayProvider),
    )

    fun utledVirkningsTidspunkt(behandlingId: BehandlingId): LocalDate? {
        val vilkårsResultat = vilkårsresultatRepository.hent(behandlingId)

        // Første periode med rett. Dette virker fordi rettighetstype-tidslinjen aldri har null-verdier.
        return vurderRettighetsType(vilkårsResultat, kvoteService.beregn()).segmenter().firstOrNull()?.periode?.fom
    }
}