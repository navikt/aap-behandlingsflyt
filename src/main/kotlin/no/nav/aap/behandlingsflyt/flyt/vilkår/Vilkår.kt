package no.nav.aap.behandlingsflyt.flyt.vilkår

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.tidslinje.Segment
import no.nav.aap.behandlingsflyt.tidslinje.StandardSammenslåere
import no.nav.aap.behandlingsflyt.tidslinje.Tidslinje
import java.time.LocalDate

class Vilkår(
    val type: Vilkårtype,
    vilkårsperioder: Set<Vilkårsperiode> = emptySet()
) {
    private var vilkårTidslinje = Tidslinje(vilkårsperioder.map { vp -> Segment(vp.periode, Vilkårsvurdering(vp)) })

    fun vilkårsperioder(): List<Vilkårsperiode> {
        return vilkårTidslinje.segmenter()
            .filter { it.verdi != null }
            .map { segment -> Vilkårsperiode(segment.periode, segment.verdi!!) }
    }

    fun leggTilVurdering(vilkårsperiode: Vilkårsperiode) {
        vilkårTidslinje = vilkårTidslinje.kombiner(
            Tidslinje(
                listOf(
                    Segment(
                        vilkårsperiode.periode,
                        Vilkårsvurdering(vilkårsperiode)
                    )
                )
            ), StandardSammenslåere.prioriterHøyreSide()
        )
    }

    fun leggTilIkkeVurdertPeriode(rettighetsperiode: Periode) {
        this.leggTilVurdering(
            Vilkårsperiode(
                periode = rettighetsperiode,
                utfall = Utfall.IKKE_VURDERT,
                manuellVurdering = false,
                faktagrunnlag = null,
                begrunnelse = null
            )
        )
    }

    fun harPerioderSomIkkeErVurdert(periodeTilVurdering: Set<Periode>): Boolean {
        return vilkårTidslinje.kryss(Tidslinje(periodeTilVurdering.map { Segment(it, Unit) }))
            .segmenter()
            .filter { it.verdi != null }
            .any { periode -> periode.verdi!!.erIkkeVurdert() }
    }

    override fun toString(): String {
        return "Vilkår(type=$type)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vilkår

        if (type != other.type) return false
        if (vilkårTidslinje != other.vilkårTidslinje) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + vilkårTidslinje.hashCode()
        return result
    }

    fun førsteDatoTilVurdering(): LocalDate {
        return vilkårTidslinje.minDato()
    }
}
