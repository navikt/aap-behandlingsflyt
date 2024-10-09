package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje

class MeldepliktFritaksperioder private constructor(private val tidslinje: Tidslinje<Fritaksvurdering>) {

    constructor(fritaksvurderinger: List<Fritaksvurdering>): this(
        fritaksvurderinger.sortedBy { it.fraDato }.fold(Tidslinje()) { acc, fritaksvurdering ->
            acc.kombiner(fritaksvurdering.tidslinje(), StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }
    )

    fun leggTil(nyeFritaksperioder: MeldepliktFritaksperioder): MeldepliktFritaksperioder {
        return MeldepliktFritaksperioder(
            tidslinje.kombiner(nyeFritaksperioder.tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        )
    }

    fun gjeldendeFritaksvurderinger(): List<Fritaksvurdering> {
        return tidslinje.komprimer().map { it.verdi.copy(fraDato = it.periode.fom) }
    }
}