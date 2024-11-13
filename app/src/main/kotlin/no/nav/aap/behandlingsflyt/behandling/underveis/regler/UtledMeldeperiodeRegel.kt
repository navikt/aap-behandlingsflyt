package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje

class UtledMeldeperiodeRegel : UnderveisRegel {
    companion object {
        const val MELDEPERIODE_LENGDE: Long = 14

        fun <T> groupByMeldeperiode(
            vurderinger: Tidslinje<Vurdering>,
            tidslinje: Tidslinje<T>,
        ): Tidslinje<Tidslinje<T>> {
            return tidslinje.splittOppIPerioder(
                vurderinger.mapNotNull { it.verdi.meldeperiode }
            )
        }
    }

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val rettighetsperiode = input.rettighetsperiode
        val meldeperiodeTidslinje = generateSequence(rettighetsperiode.fom) { it.plusDays(MELDEPERIODE_LENGDE) }
            .takeWhile { it <= rettighetsperiode.tom }
            .map { Periode(it, minOf(it.plusDays(MELDEPERIODE_LENGDE - 1), rettighetsperiode.tom)) }
            .map { Segment(it, it) }
            .toList()
            .let { Tidslinje(it) }

        return resultat.leggTilVurderinger(meldeperiodeTidslinje, Vurdering::leggTilMeldeperiode)
    }
}