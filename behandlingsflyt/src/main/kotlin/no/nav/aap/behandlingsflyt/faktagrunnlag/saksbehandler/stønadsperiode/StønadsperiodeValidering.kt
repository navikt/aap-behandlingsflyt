package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav

object StønadsperiodeValidering {

    fun evaluerTilstrekkeligVurdert(
        relevanteKravVurderinger: Set<RelevantKrav>,
        stønadsperiodeVurderinger: Set<StønadsperiodeVurdering>
    ): EvaluerTilstrekkeligVurdert {
        val kravSomKreverVurdering = relevanteKravVurderinger.map { it.referanse }.toSet()
        val kravSomHarVurdering = stønadsperiodeVurderinger.map { it.referanse }.toSet()

        val vurderingerForIkkeRelevanteKrav = kravSomHarVurdering - kravSomKreverVurdering
        val kravSomManglerVurdering = kravSomKreverVurdering - kravSomHarVurdering

        return when {
            vurderingerForIkkeRelevanteKrav.isEmpty() && kravSomManglerVurdering.isEmpty() -> TilstrekkeligVurdert

            vurderingerForIkkeRelevanteKrav.isNotEmpty() && kravSomManglerVurdering.isNotEmpty() -> IkkeTilstrekkeligVurdert(
                "Det finnes vurderinger for krav som ikke er relevante: ${vurderingerForIkkeRelevanteKrav.joinToString(", ")}. " +
                        "Det finnes krav som mangler vurdering: ${kravSomManglerVurdering.joinToString(", ")}",
            )

            vurderingerForIkkeRelevanteKrav.isNotEmpty() -> IkkeTilstrekkeligVurdert(
                "Det finnes vurderinger for krav som ikke er relevante: ${
                    vurderingerForIkkeRelevanteKrav.joinToString(
                        ", "
                    )
                }",
            )

            else -> IkkeTilstrekkeligVurdert(
                "Det finnes krav som mangler vurdering: ${kravSomManglerVurdering.joinToString(", ")}",
            )
        }
    }
}


sealed interface EvaluerTilstrekkeligVurdert

data object TilstrekkeligVurdert : EvaluerTilstrekkeligVurdert

data class IkkeTilstrekkeligVurdert(
    val melding: String,
) : EvaluerTilstrekkeligVurdert