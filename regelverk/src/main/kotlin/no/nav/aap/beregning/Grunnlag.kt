package no.nav.aap.beregning

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