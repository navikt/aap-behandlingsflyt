package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering.Companion.tidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering.FritaksvurderingData
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje

class MeldepliktFritaksperioder private constructor(private val tidslinje: Tidslinje<FritaksvurderingData>) {

    constructor(fritaksvurderinger: List<Fritaksvurdering>) : this(fritaksvurderinger.tidslinje())

    fun leggTil(nyeFritaksperioder: MeldepliktFritaksperioder): MeldepliktFritaksperioder {
        return MeldepliktFritaksperioder(
            tidslinje.kombiner(nyeFritaksperioder.tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        )
    }

    fun gjeldendeFritaksvurderinger(): List<Fritaksvurdering> {
        return tidslinje.komprimer()
            .map { Fritaksvurdering(
                harFritak = it.verdi.harFritak,
                fraDato = it.periode.fom,
                begrunnelse = it.verdi.begrunnelse,
                vurdertAv = it.verdi.vurdertAv,
                opprettetTid = it.verdi.opprettetTid,
            ) }
    }
}