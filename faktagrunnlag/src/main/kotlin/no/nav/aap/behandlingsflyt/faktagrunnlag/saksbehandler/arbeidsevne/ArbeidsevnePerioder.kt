package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering.ArbeidsevneVurderingData
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering.Companion.tidslinje

class ArbeidsevnePerioder private constructor(private val tidslinje: Tidslinje<ArbeidsevneVurderingData>) {

    constructor(fritaksvurderinger: List<ArbeidsevneVurdering>): this(fritaksvurderinger.tidslinje())

    fun leggTil(nyeFritaksperioder: ArbeidsevnePerioder): ArbeidsevnePerioder {
        return ArbeidsevnePerioder(
            tidslinje.kombiner(nyeFritaksperioder.tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        )
    }

    fun gjeldendeArbeidsevner(): List<ArbeidsevneVurdering> {
        return tidslinje.komprimer().map { it.verdi.toArbeidsevneVurdering(it.periode.fom) }
    }
}