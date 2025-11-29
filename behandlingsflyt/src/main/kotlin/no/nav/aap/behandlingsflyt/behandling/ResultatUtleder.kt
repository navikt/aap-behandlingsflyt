package no.nav.aap.behandlingsflyt.behandling

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

enum class Resultat {
    INNVILGELSE,
    AVSLAG,
    TRUKKET,
    AVBRUTT
}


class ResultatUtleder(
    private val underveisRepository: UnderveisRepository,
    private val behandlingRepository: BehandlingRepository,
    private val trukketSøknadService: TrukketSøknadService,
    private val avbrytRevurderingService: AvbrytRevurderingService
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        underveisRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider)
    )

    fun utledResultat(behandlingId: BehandlingId): Resultat {
        val behandling = behandlingRepository.hent(behandlingId)
        return utledResultatFørstegangsBehandling(behandling)
    }

    fun utledRevurderingResultat(behandlingId: BehandlingId): Resultat? {
        val behandling = behandlingRepository.hent(behandlingId)
        return utledResultatRevurderingsBehandling(behandling)
    }

    fun utledResultatRevurderingsBehandling(behandling: Behandling): Resultat? {
        require(behandling.typeBehandling() == TypeBehandling.Revurdering) {
            "Kan ikke utlede resultat for ${behandling.typeBehandling()} ennå."
        }

        if (avbrytRevurderingService.revurderingErAvbrutt(behandling.id)) {
            return Resultat.AVBRUTT
        }

        return null
    }

    @WithSpan
    fun utledResultatFørstegangsBehandling(behandling: Behandling): Resultat {

        require(behandling.typeBehandling() == TypeBehandling.Førstegangsbehandling) {
            "Kan ikke utlede resultat for ${behandling.typeBehandling()} ennå."
        }

        if (trukketSøknadService.søknadErTrukket(behandling.id)) {
            return Resultat.TRUKKET
        }

        val harOppfyltPeriode = underveisRepository.hentHvisEksisterer(behandling.id)
            ?.perioder
            .orEmpty()
            .any { it.utfall == Utfall.OPPFYLT }

        return if (harOppfyltPeriode) Resultat.INNVILGELSE else Resultat.AVSLAG
    }

    fun erRentAvslag(behandling: Behandling): Boolean {

        if (trukketSøknadService.søknadErTrukket(behandling.id)) {
            return false
        }

        val harOppfyltPeriode = underveisRepository.hentHvisEksisterer(behandling.id)
            ?.perioder
            .orEmpty()
            .any { it.utfall == Utfall.OPPFYLT }

        return if (harOppfyltPeriode) false else true
    }
}