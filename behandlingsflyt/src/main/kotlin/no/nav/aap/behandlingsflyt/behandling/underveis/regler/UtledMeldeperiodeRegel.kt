package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode

class UtledMeldeperiodeRegel : UnderveisRegel {
    companion object {
        const val MELDEPERIODE_LENGDE: Long = 14

        fun <T> groupByMeldeperiode(
            vurderinger: Tidslinje<Vurdering>,
            tidslinje: Tidslinje<T>,
        ): Tidslinje<Tidslinje<T>> {
            return tidslinje.splittOppIPerioder(
                vurderinger.segmenter().map {
                    Periode(
                        fom = maxOf(it.verdi.meldeperiode().fom, vurderinger.minDato()),
                        tom = minOf(it.verdi.meldeperiode().tom, vurderinger.maxDato()),
                    )
                }
            )
        }
    }

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val meldeperiodeTidslinje = Tidslinje(input.meldeperioder.map { Segment(it, it) })
            .begrensetTil(input.rettighetsperiode)

        return resultat.leggTilVurderinger(meldeperiodeTidslinje, Vurdering::leggTilMeldeperiode)
    }
}