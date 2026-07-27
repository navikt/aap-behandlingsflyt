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
        
        if (vurderingerForIkkeRelevanteKrav.isNotEmpty() || kravSomManglerVurdering.isNotEmpty()) {
            val melding = buildString {
                if (vurderingerForIkkeRelevanteKrav.isNotEmpty()) {
                    append("Det finnes vurderinger for krav som ikke er relevante: ")
                    append(vurderingerForIkkeRelevanteKrav.formaterReferanse())
                }
                if (kravSomManglerVurdering.isNotEmpty()) {
                    if (isNotEmpty()) append(". ")
                    append("Det finnes krav som mangler vurdering: ")
                    append(kravSomManglerVurdering.formaterReferanse())
                }
            }
            return IkkeTilstrekkeligVurdert(melding)
        }

        val kravsegmenter = relevanteKravVurderinger.kravtidslinje().segmenter()
        val vurderingerMedUgyldigDato = stønadsperiodeVurderinger.filterNot { vurdering ->
            val kravSegment = kravsegmenter.single { it.verdi.referanse == vurdering.referanse }
            kravSegment.periode.inneholder(vurdering.startDato)
        }

        return if (vurderingerMedUgyldigDato.isNotEmpty()) {
            IkkeTilstrekkeligVurdert(
                "Startdato må være innenfor perioden for kravet: ${vurderingerMedUgyldigDato.formaterStønadsperioder()}"
            )
        } else {
            TilstrekkeligVurdert
        }
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