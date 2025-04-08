package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

enum class Resultat {
    INNVILGELSE,
    AVSLAG
}

class ResultatUtleder(
    private val underveisRepository: UnderveisRepository,
) {
    fun utledResultat(behandlingId: BehandlingId): Resultat {
        val underveisGrunnlag = underveisRepository.hent(behandlingId)

        val oppfyltePerioder = underveisGrunnlag.perioder.filter { it.utfall == Utfall.OPPFYLT }

        return if (oppfyltePerioder.isNotEmpty()) {
            Resultat.INNVILGELSE
        } else {
            Resultat.AVSLAG
        }
    }
}