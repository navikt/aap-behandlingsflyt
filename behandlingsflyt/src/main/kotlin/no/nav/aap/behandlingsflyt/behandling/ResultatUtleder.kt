package no.nav.aap.behandlingsflyt.behandling

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.underveis.UnderveisService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

enum class Resultat {
    INNVILGELSE,
    AVSLAG,
    TRUKKET,
    AVBRUTT
}


class ResultatUtleder(
    private val behandlingRepository: BehandlingRepository,
    private val trukketSøknadService: TrukketSøknadService,
    private val avbrytRevurderingService: AvbrytRevurderingService,
    private val underveisService: UnderveisService,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider),
        underveisService = UnderveisService(repositoryProvider, gatewayProvider),
    )

    fun utledResultat(behandling: Behandling): Resultat? {
        return when (behandling.typeBehandling()) {
            TypeBehandling.Førstegangsbehandling ->
                utledResultatFørstegangsBehandling(behandling)

            TypeBehandling.Revurdering ->
                utledResultatRevurderingsBehandling(behandling)

            else ->
                error("Kan kun utlede resultat for ytelsesbehandlinger")
        }
    }

    fun utledRevurderingResultat(behandlingId: BehandlingId): Resultat? {
        val behandling = behandlingRepository.hent(behandlingId)
        return utledResultatRevurderingsBehandling(behandling)
    }

    fun utledResultatRevurderingsBehandling(behandling: Behandling): Resultat? {
        require(behandling.typeBehandling() == TypeBehandling.Revurdering) {
            "Kan ikke utlede resultat for ${behandling.typeBehandling()} ennå."
        }
        val forrigeBehandlingId = requireNotNull(behandling.forrigeBehandlingId) {
            "Revurdering skal alltid ha forrigeBehandlingId, men ${behandling.id} mangler det."
        }

        if (avbrytRevurderingService.revurderingErAvbrutt(behandling.id)) {
            return Resultat.AVBRUTT
        }

        val søknadMottatt = behandling.vurderingsbehov().any { it.type == Vurderingsbehov.MOTTATT_SØKNAD }
        if (!harRett(forrigeBehandlingId) && søknadMottatt) {
            return if (harRett(behandling.id))
                Resultat.INNVILGELSE
            else
                Resultat.AVSLAG
        }

        return null
    }

    fun utledResultatFørstegangsBehandling(behandlingId: BehandlingId): Resultat {
        val behandling = behandlingRepository.hent(behandlingId)
        return utledResultatFørstegangsBehandling(behandling)
    }

    @WithSpan
    fun utledResultatFørstegangsBehandling(behandling: Behandling): Resultat {
        require(behandling.typeBehandling() == TypeBehandling.Førstegangsbehandling) {
            "Kan ikke utlede resultat for ${behandling.typeBehandling()} ennå."
        }

        if (trukketSøknadService.søknadErTrukket(behandling.id)) {
            return Resultat.TRUKKET
        }

        val harOppfyltPeriode = harRett(behandling.id)

        return if (harOppfyltPeriode) Resultat.INNVILGELSE else Resultat.AVSLAG
    }

    fun erRentAvslag(behandling: Behandling): Boolean {

        if (trukketSøknadService.søknadErTrukket(behandling.id)) {
            return false
        }

        val harOppfyltPeriode = harRett(behandling.id)

        return !harOppfyltPeriode
    }

    fun harRett(behandlingId: BehandlingId) =
        underveisService.rettighetsType(behandlingId).isNotEmpty()
}