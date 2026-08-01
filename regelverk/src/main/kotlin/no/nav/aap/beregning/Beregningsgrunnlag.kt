package no.nav.aap.beregning

import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.komponenter.verdityper.GUnit

/**
 * Grunnlag for beregningen.
 */
sealed interface Beregningsgrunnlag : Grunnlag {
    /**
     * Hvilket grunnlag som blir brukt som grunnlag for AAP-beregningen.
     */
    override fun grunnlaget(): GUnit
    /**
     * Brukt til serialisering. // TODO: bedre docstring
     */
    fun faktagrunnlag(): Faktagrunnlag
}