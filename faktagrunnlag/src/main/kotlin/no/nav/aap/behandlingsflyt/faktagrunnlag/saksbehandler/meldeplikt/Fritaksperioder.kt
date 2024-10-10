package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering.FritaksvurderingData
import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje

class Fritaksperioder private constructor(private val tidslinje: Tidslinje<FritaksvurderingData>) {

    constructor(fritaksvurderinger: List<Fritaksvurdering>) : this(
        fritaksvurderinger.sortedBy { it.fraDato }
            .fold(Tidslinje<FritaksvurderingData>()) { acc, fritaksvurdering ->
                acc.kombiner(fritaksvurdering.tidslinje(), StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }
    )

    fun leggTil(nyeFritaksperioder: Fritaksperioder): Fritaksperioder {
        return Fritaksperioder(
            tidslinje.kombiner(nyeFritaksperioder.tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        )
    }

    fun gjeldendeFritaksvurderinger(): List<Fritaksvurdering> {
        return tidslinje.komprimer()
            .map { Fritaksvurdering(it.verdi.harFritak, it.periode.fom, it.verdi.begrunnelse, it.verdi.opprettetTid) }
    }
}