package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje

class ArbeidsevnePerioder private constructor(private val tidslinje: Tidslinje<ArbeidsevneVurdering>) {

    constructor(fritaksvurderinger: List<ArbeidsevneVurdering>): this(
        fritaksvurderinger.sortedBy { it.fraDato }.fold(Tidslinje()) { acc, arbeidsevneVurdering ->
            acc.kombiner(arbeidsevneVurdering.tidslinje(), StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }
    )

    fun leggTil(nyeFritaksperioder: ArbeidsevnePerioder): ArbeidsevnePerioder {
        return ArbeidsevnePerioder(
            tidslinje.kombiner(nyeFritaksperioder.tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        )
    }

    fun gjeldendeArbeidsevner(): List<ArbeidsevneVurdering> {
        return tidslinje.komprimer().map { it.verdi.copy(fraDato = it.periode.fom) }
    }
}