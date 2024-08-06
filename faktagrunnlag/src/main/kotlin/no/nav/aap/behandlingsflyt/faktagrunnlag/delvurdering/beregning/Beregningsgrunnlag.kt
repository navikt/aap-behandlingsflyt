package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag
import no.nav.aap.verdityper.GUnit

/**
 * Grunnlag for beregningen.
 */
sealed interface Beregningsgrunnlag {
    /**
     * Hvilket grunnlag som blir brukt som grunnlag for AAP-beregningen.
     */
    fun grunnlaget(): GUnit
    /**
     * Brukt til serialisering. // TODO: bedre docstring
     */
    fun faktagrunnlag(): Faktagrunnlag
    /**
     * Om minst én inntekt i beregningen er begrenset oppdag til 6G.
     */
    fun er6GBegrenset(): Boolean
    /**
     * Om et gjennomnsnitt av flere års inntekter er brukt i beregningen.
     */
    fun erGjennomsnitt(): Boolean
}
