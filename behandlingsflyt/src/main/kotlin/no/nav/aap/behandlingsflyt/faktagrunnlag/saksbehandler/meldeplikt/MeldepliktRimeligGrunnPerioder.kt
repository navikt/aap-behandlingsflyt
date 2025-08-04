package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.RimeligGrunnVurdering.Companion.tidslinje
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje

class MeldepliktRimeligGrunnPerioder private constructor(private val tidslinje: Tidslinje<RimeligGrunnVurdering.RimeligGrunnVurderingData>) {

    constructor(rimeligGrunnVurderinger: List<RimeligGrunnVurdering>) : this(rimeligGrunnVurderinger.tidslinje())

    fun leggTil(nyeRimeligGrunnPerioder: MeldepliktRimeligGrunnPerioder): MeldepliktRimeligGrunnPerioder {
        return MeldepliktRimeligGrunnPerioder(
            tidslinje.kombiner(nyeRimeligGrunnPerioder.tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        )
    }

    fun gjeldendeRimeligGrunnVurderinger(): List<RimeligGrunnVurdering> {
        return tidslinje.komprimer()
            .map { RimeligGrunnVurdering(
                harRimeligGrunn = it.verdi.harRimeligGrunn,
                fraDato = it.periode.fom,
                begrunnelse = it.verdi.begrunnelse,
                vurdertAv = it.verdi.vurdertAv,
                opprettetTid = it.verdi.opprettetTid,
            ) }
    }
}