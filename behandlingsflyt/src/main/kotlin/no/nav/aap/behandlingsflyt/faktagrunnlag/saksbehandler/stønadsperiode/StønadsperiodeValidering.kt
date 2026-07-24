package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.kravtidslinje

object StønadsperiodeValidering {

    fun evaluerTilstrekkeligVurdert(
        relevanteKravVurderinger: Set<RelevantKrav>, stønadsperiodeVurderinger: Set<StønadsperiodeVurdering>
    ): EvaluerTilstrekkeligVurdert {
        val kravSomKreverVurdering = relevanteKravVurderinger.map { it.referanse }.toSet()
        val kravSomHarVurdering = stønadsperiodeVurderinger.map { it.referanse }.toSet()

        val vurderingerForIkkeRelevanteKrav = kravSomHarVurdering - kravSomKreverVurdering
        val kravSomManglerVurdering = kravSomKreverVurdering - kravSomHarVurdering


        when {
            vurderingerForIkkeRelevanteKrav.isNotEmpty() && kravSomManglerVurdering.isNotEmpty() -> return IkkeTilstrekkeligVurdert(
                "Det finnes vurderinger for krav som ikke er relevante: ${vurderingerForIkkeRelevanteKrav.formaterReferanse()}. " + "Det finnes krav som mangler vurdering: ${kravSomManglerVurdering.formaterReferanse()}",
            )

            vurderingerForIkkeRelevanteKrav.isNotEmpty() -> return IkkeTilstrekkeligVurdert(
                "Det finnes vurderinger for krav som ikke er relevante: ${
                    vurderingerForIkkeRelevanteKrav.formaterReferanse()
                }",
            )

            kravSomManglerVurdering.isNotEmpty() -> return IkkeTilstrekkeligVurdert(
                "Det finnes krav som mangler vurdering: ${kravSomManglerVurdering.formaterReferanse()}",
            )
        }

        val vurderingerMedUgyldigDato = stønadsperiodeVurderinger.filterNot {
            val kravSegment = relevanteKravVurderinger.kravtidslinje().segmenter()
                .single { segment -> segment.verdi.referanse == it.referanse }
            kravSegment.periode.inneholder(it.startDato)
        }

        if (vurderingerMedUgyldigDato.isNotEmpty()) {
            return IkkeTilstrekkeligVurdert(
                "Startdato må være innenfor perioden for kravet: ${
                    vurderingerMedUgyldigDato.formaterStønadsperioder()
                }"
            )
        }

        return TilstrekkeligVurdert
    }

    private fun Collection<Kravreferanse>.formaterReferanse(): String {
        return this.joinToString(", ") { it.verdi.toString() }
    }

    private fun Collection<StønadsperiodeVurdering>.formaterStønadsperioder(): String {
        return this.map { it.referanse }.formaterReferanse()
    }
}


sealed interface EvaluerTilstrekkeligVurdert

data object TilstrekkeligVurdert : EvaluerTilstrekkeligVurdert

data class IkkeTilstrekkeligVurdert(
    val melding: String,
) : EvaluerTilstrekkeligVurdert