package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping

import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingVurdering.ArbeidsopptrappingVurderingData
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingVurdering.Companion.tidslinje
import no.nav.aap.komponenter.tidslinje.Tidslinje

class ArbeidsopptrappingPerioder private constructor(private val tidslinje: Tidslinje<ArbeidsopptrappingVurderingData>) {

    constructor(arbeidsopptrappingVurderinger: List<ArbeidsopptrappingVurdering>) : this(arbeidsopptrappingVurderinger.tidslinje())

    fun leggTil(nyeFritaksperioder: ArbeidsopptrappingPerioder): ArbeidsopptrappingPerioder {
        return ArbeidsopptrappingPerioder(
            tidslinje.kombiner(nyeFritaksperioder.tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        )
    }

    fun gjeldendeArbeidsopptrappingsVurderinger(): List<ArbeidsopptrappingVurdering> {
        return tidslinje.komprimer()
            .segmenter()
            .map {
                ArbeidsopptrappingVurdering(
                    reellMulighetTilOpptrapping = it.verdi.reellMulighetTilOpptrapping,
                    rettPaaAAPIOpptrapping = it.verdi.rettPaaAAPIOpptrapping,
                    fraDato = it.periode.fom,
                    begrunnelse = it.verdi.begrunnelse,
                    vurdertAv = it.verdi.vurdertAv,
                    opprettetTid = it.verdi.opprettetTid,
                )
            }
    }
}