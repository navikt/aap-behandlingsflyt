package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje

class Fritaksperioder private constructor(private val tidslinje: Tidslinje<Fritaksvurdering>) {

    constructor(fritaksvurderinger: List<Fritaksvurdering>): this(
        fritaksvurderinger.drop(1).fold(fritaksvurderinger.first().tidslinje()) { acc, fritaksvurdering ->
            acc.kombiner(fritaksvurdering.tidslinje(), StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }
    )

    fun leggTil(nyeFritaksperioder: Fritaksperioder): List<Fritaksvurdering> {
        return tidslinje.kombiner(nyeFritaksperioder.tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            .komprimer().map { it.verdi.copy(fraDato = it.periode.fom) }
    }
}