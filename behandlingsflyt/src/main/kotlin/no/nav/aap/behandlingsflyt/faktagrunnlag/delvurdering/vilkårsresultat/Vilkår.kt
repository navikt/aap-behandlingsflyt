package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDateTime

class Vilkår(
    val type: Vilkårtype,
    vilkårsperioder: Set<Vilkårsperiode> = emptySet(),
    val vurdertTidspunkt: LocalDateTime? = null
) {
    init {
        validerKombinasjon(type, vilkårsperioder)
    }

    private fun validerKombinasjon(
        type: Vilkårtype,
        vilkårsperioder: Set<Vilkårsperiode> = emptySet()
    ) {
        require(
            vilkårsperioder.mapNotNull { it.innvilgelsesårsak }.all { it in type.spesielleInnvilgelsesÅrsaker }) {
            "Spesielle innvilgelsesårsaker må være definert i VilkårType. " +
                    "Gyldige innvilgelsesårsaker: ${type.spesielleInnvilgelsesÅrsaker.joinToString { it.name }} for vilkår $type"
        }
        require(vilkårsperioder.mapNotNull { it.avslagsårsak }
            .all { it in type.avslagsårsaker }) {
            "Ugyldig avslagsårsak for $type, avslagsårsak: ${
                vilkårsperioder.mapNotNull { it.avslagsårsak }
                    .filterNot { it in type.avslagsårsaker }
            }. " +
                    "Gyldige avslagsårsaker: ${type.avslagsårsaker.joinToString { it.name }}"
        }
    }

    private var vilkårTidslinje = Tidslinje(vilkårsperioder.map { vp -> Segment(vp.periode, Vilkårsvurdering(vp)) })

    fun tidslinje(): Tidslinje<Vilkårsvurdering> {
        return vilkårTidslinje
    }

    fun vilkårsperioder(): List<Vilkårsperiode> {
        return vilkårTidslinje.segmenter()
            .map { segment -> Vilkårsperiode(segment.periode, segment.verdi) }
    }

    fun leggTilVurderinger(tidslinje: Tidslinje<Vilkårsvurdering>) {
        vilkårTidslinje = vilkårTidslinje.kombiner(
            tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin()
        )
    }

    fun leggTilVurdering(vilkårsperiode: Vilkårsperiode) {
        validerKombinasjon(type, setOf(vilkårsperiode))
        vilkårTidslinje = vilkårTidslinje.kombiner(
            Tidslinje(
                listOf(
                    Segment(
                        vilkårsperiode.periode,
                        Vilkårsvurdering(vilkårsperiode)
                    )
                )
            ), StandardSammenslåere.prioriterHøyreSideCrossJoin()
        )
    }

    fun leggTilIkkeVurdertPeriode(rettighetsperiode: Periode): Vilkår {
        this.leggTilVurdering(
            Vilkårsperiode(
                periode = rettighetsperiode,
                utfall = Utfall.IKKE_VURDERT,
                manuellVurdering = false,
                begrunnelse = null,
                faktagrunnlag = null
            )
        )
        return this
    }

    fun fjernHvisUtenforRettighetsperiode(rettighetsperiode: Periode): Vilkår {
        vilkårTidslinje = vilkårTidslinje.begrensetTil(rettighetsperiode)
        return this
    }

    fun nullstillTidslinje(): Vilkår {
        vilkårTidslinje = Tidslinje(emptyList())
        return this
    }

    fun harPerioderSomIkkeErVurdert(periodeTilVurdering: Set<Periode>): Boolean {
        return vilkårTidslinje.kryss(Tidslinje(periodeTilVurdering.map { Segment(it, Unit) }))
            .segmenter()
            .any { periode -> periode.verdi.erIkkeVurdert() }
    }

    fun harPerioderSomErOppfylt(): Boolean {
        return vilkårTidslinje.segmenter().any { it.verdi.erOppfylt() }
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

    override fun toString(): String {
        return "Vilkår(type=$type, vilkårTidslinje=$vilkårTidslinje)"
    }

    fun harPerioderSomIkkeErOppfylt(): Boolean {
        return vilkårsperioder().any { ! it.erOppfylt() }
    }
}