package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering.ArbeidsevneVurderingData
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering.Companion.tidslinje
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje

class ArbeidsevnePerioder private constructor(
    private val tidslinje: Tidslinje<ArbeidsevneVurderingData>
) {
    constructor(fritaksvurderinger: List<ArbeidsevneVurdering>) : this(fritaksvurderinger.tidslinje())

    fun leggTil(nyeFritaksperioder: ArbeidsevnePerioder): ArbeidsevnePerioder =
        ArbeidsevnePerioder(
            tidslinje.kombiner(nyeFritaksperioder.tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        )

    fun leggTil(arbeidsevneVurderinger: List<ArbeidsevneVurdering>): ArbeidsevnePerioder =
        leggTil(ArbeidsevnePerioder(arbeidsevneVurderinger.tidslinje()))

    fun gjeldendeArbeidsevner(): List<ArbeidsevneVurdering> =
        tidslinje.komprimer().segmenter().map {
            it.verdi.toArbeidsevneVurdering(it.periode.fom)
        }
}