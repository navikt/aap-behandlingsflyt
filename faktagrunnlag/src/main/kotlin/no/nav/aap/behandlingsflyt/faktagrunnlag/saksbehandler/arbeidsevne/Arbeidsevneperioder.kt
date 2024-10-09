package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje

class Arbeidsevneperioder private constructor(private val tidslinje: Tidslinje<Arbeidsevnevurdering>) {

    constructor(fritaksvurderinger: List<Arbeidsevnevurdering>): this(
        fritaksvurderinger.sortedBy { it.fraDato }.fold(Tidslinje()) { acc, arbeidsevnevurdering ->
            acc.kombiner(arbeidsevnevurdering.tidslinje(), StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }
    )

    fun leggTil(nyeFritaksperioder: Arbeidsevneperioder): Arbeidsevneperioder {
        return Arbeidsevneperioder(
            tidslinje.kombiner(nyeFritaksperioder.tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        )
    }

    fun gjeldendeArbeidsevner(): List<Arbeidsevnevurdering> {
        return tidslinje.komprimer().map { it.verdi.copy(fraDato = it.periode.fom) }
    }
}