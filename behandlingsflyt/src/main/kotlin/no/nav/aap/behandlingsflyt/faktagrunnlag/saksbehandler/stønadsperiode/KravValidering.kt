package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav

object StønadsperiodeValidering {
    fun erTilstrekkeligVurdert(
        relevanteKravVurderinger: Set<RelevantKrav>,
        stønadsperiodeVurderinger: Set<StønadsperiodeVurdering>
    ): Boolean {
        val erAlleKravVurdert =
            relevanteKravVurderinger.all { krav -> stønadsperiodeVurderinger.any { it.referanse == krav.referanse } }

        return erAlleKravVurdert
    }
}