package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.KansellerRevurderingService
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
    KANSELLERT
}


class ResultatUtleder(
    private val underveisRepository: UnderveisRepository,
    private val behandlingRepository: BehandlingRepository,
    private val trukketSøknadService: TrukketSøknadService,
    private val kansellerRevurderingService: KansellerRevurderingService
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        underveisRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        kansellerRevurderingService = KansellerRevurderingService(repositoryProvider)
    )

    fun utledResultat(behandlingId: BehandlingId): Resultat {
        val behandling = behandlingRepository.hent(behandlingId)
        return utledResultatFørstegangOgRevurderingsBehandling(behandling)
    }

    fun utledResultatFørstegangOgRevurderingsBehandling(behandling: Behandling): Resultat {

        require(behandling.typeBehandling() in listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)) {
            "Kan ikke utlede resultat for ${behandling.typeBehandling()} ennå."
        }

        if (trukketSøknadService.søknadErTrukket(behandling.id)) {
            return Resultat.TRUKKET
        }

        if (kansellerRevurderingService.revurderingErKansellert(behandling.id)) {
            return Resultat.KANSELLERT
        }

        val harOppfyltPeriode = underveisRepository.hentHvisEksisterer(behandling.id)
            ?.perioder
            .orEmpty()
            .any { it.utfall == Utfall.OPPFYLT }

        return if (harOppfyltPeriode) Resultat.INNVILGELSE else Resultat.AVSLAG
    }
}