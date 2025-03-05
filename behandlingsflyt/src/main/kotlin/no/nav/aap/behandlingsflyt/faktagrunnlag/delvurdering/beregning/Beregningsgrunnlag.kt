package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.komponenter.verdityper.GUnit

/**
 * Om man kun er interessert i grunnlaget, og ikke konkrete klasser som implementerer det lukkede  interfaces [Beregningsgrunnlag] kan denne klassen
 * brukes som parameter.
 *
 * Gjør det enklere å lage testdata, og begrenser informasjon.
 */
interface Grunnlag {
    fun grunnlaget(): GUnit
}

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
