package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

enum class Resultat {
    INNVILGELSE,
    AVSLAG
}


class ResultatUtleder(
    private val underveisRepository: UnderveisRepository,
    private val behandlingRepository: BehandlingRepository
) {
    constructor(repositoryProvider: RepositoryProvider): this(
        underveisRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    fun utledResultat(behandlingId: BehandlingId): Resultat {
        val behandling = behandlingRepository.hent(behandlingId)

        require(behandling.typeBehandling() == TypeBehandling.Førstegangsbehandling)
        { "Kan ikke utlede resultat for ${behandling.typeBehandling()} ennå." }

        val underveisGrunnlag = underveisRepository.hent(behandlingId)

        val oppfyltePerioder = underveisGrunnlag.perioder.filter { it.utfall == Utfall.OPPFYLT }

        return if (oppfyltePerioder.isNotEmpty()) {
            Resultat.INNVILGELSE
        } else {
            Resultat.AVSLAG
        }
    }
}