package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

data class KravGrunnlag(
    val vurderinger: Set<KravVurdering>,
) {
    fun gjeldendeVurderinger(): Set<KravVurdering> {
        return vurderinger
            .groupBy { it.referanse }
            .values
            .map { kravForReferanse -> kravForReferanse.maxBy { it.opprettet } }
            .toSet()
    }
}

