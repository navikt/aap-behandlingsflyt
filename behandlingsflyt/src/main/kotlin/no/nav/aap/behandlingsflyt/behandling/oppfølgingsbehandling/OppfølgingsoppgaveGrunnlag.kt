package no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov

enum class KonsekvensAvOppfølging {
    INGEN,
    OPPRETT_VURDERINGSBEHOV
}

/**
 * Til bruk i løser/løsning. Klassen [OppfølgingsoppgaveGrunnlag] er til databasen og internt i kodebasen.
 */
data class OppfølgingsoppgaveGrunnlagDto(
    val konsekvensAvOppfølging: KonsekvensAvOppfølging,
    val opplysningerTilRevurdering: List<Vurderingsbehov>? = emptyList(),
    val årsak: String? = null,
) {
    fun tilOppfølgingsoppgaveGrunnlag(vurdertAv: String): OppfølgingsoppgaveGrunnlag {
        return OppfølgingsoppgaveGrunnlag(
            konsekvensAvOppfølging = konsekvensAvOppfølging,
            opplysningerTilRevurdering = opplysningerTilRevurdering.orEmpty(),
            årsak = årsak,
            vurdertAv = vurdertAv,
        )
    }
}

data class OppfølgingsoppgaveGrunnlag(
    val konsekvensAvOppfølging: KonsekvensAvOppfølging,
    val opplysningerTilRevurdering: List<Vurderingsbehov>,
    val årsak: String?,
    val vurdertAv: String
) {
    init {
        if (konsekvensAvOppfølging != KonsekvensAvOppfølging.INGEN) {
            requireNotNull(årsak)
            require(opplysningerTilRevurdering.isNotEmpty())
        }
    }
}
