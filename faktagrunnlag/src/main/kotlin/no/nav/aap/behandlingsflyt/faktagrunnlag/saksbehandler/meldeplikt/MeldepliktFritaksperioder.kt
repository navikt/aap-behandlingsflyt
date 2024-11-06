package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering.Companion.tidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering.FritaksvurderingData
import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje

class MeldepliktFritaksperioder private constructor(private val tidslinje: Tidslinje<FritaksvurderingData>) {

    constructor(fritaksvurderinger: List<Fritaksvurdering>) : this(fritaksvurderinger.tidslinje())

    fun leggTil(nyeFritaksperioder: MeldepliktFritaksperioder): MeldepliktFritaksperioder {
        return MeldepliktFritaksperioder(
            tidslinje.kombiner(nyeFritaksperioder.tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        )
    }

    fun gjeldendeFritaksvurderinger(): List<Fritaksvurdering> {
        return tidslinje.komprimer()
            .map { Fritaksvurdering(it.verdi.harFritak, it.periode.fom, it.verdi.begrunnelse, it.verdi.opprettetTid) }
    }
}