package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag
import no.nav.aap.komponenter.verdityper.GUnit

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
}
