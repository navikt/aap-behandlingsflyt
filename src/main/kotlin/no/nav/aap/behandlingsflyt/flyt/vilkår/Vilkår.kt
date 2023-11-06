package no.nav.aap.behandlingsflyt.flyt.vilkår

import no.nav.aap.behandlingsflyt.Periode

class Vilkår(
    val type: Vilkårtype,
    private val vilkårsperioder: MutableSet<Vilkårsperiode> = mutableSetOf()
) {

    fun vilkårsperioder(): List<Vilkårsperiode> {
        return this.vilkårsperioder.toList()
    }

    fun leggTilVurdering(vilkårsperiode: Vilkårsperiode) {
        if (vilkårsperioder.any { vp -> vp.periode.overlapper(vilkårsperiode.periode) }) {
            // Overlapper og må justere innholdet i listen
            val justertePerioder = vilkårsperioder
                .filter { vp -> vp.periode.overlapper(vilkårsperiode.periode) }
                .filter { vp ->
                    vp.periode.tom > vilkårsperiode.periode.tom || vp.periode.fom < vilkårsperiode.periode.fom
                }
                .map { vp ->
                    Vilkårsperiode(
                        justerPeriode(vp.periode, vilkårsperiode.periode),
                        vp.utfall,
                        vp.manuellVurdering,
                        vp.begrunnelse,
                        vp.innvilgelsesårsak,
                        vp.avslagsårsak,
                        vp.faktagrunnlag,
                        vp.versjon
                    )
                }

            this.vilkårsperioder.removeIf { vp -> vp.periode.overlapper(vilkårsperiode.periode) }
            this.vilkårsperioder.addAll(justertePerioder)
            this.vilkårsperioder.add(vilkårsperiode)
        } else {
            this.vilkårsperioder.add(vilkårsperiode)
        }
    }

    internal fun justerPeriode(leftPeriode: Periode, rightPeriode: Periode): Periode {
        if (!leftPeriode.overlapper(rightPeriode)) {
            return leftPeriode
        }
        val fom = if (rightPeriode.fom.isBefore(leftPeriode.fom)) {
            rightPeriode.tom.plusDays(1)
        } else {
            leftPeriode.fom
        }
        val tom = if (rightPeriode.tom.isAfter(leftPeriode.tom)) {
            rightPeriode.fom.minusDays(1)
        } else {
            leftPeriode.tom
        }
        return Periode(fom, tom)
    }

    fun leggTilIkkeVurdertPeriode(rettighetsperiode: Periode) {
        this.vilkårsperioder.add(
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
        return this.vilkårsperioder
            .filter { periode -> periodeTilVurdering.any { vp -> periode.periode.overlapper(vp) } }
            .any { periode -> periode.erIkkeVurdert() }

    }

    override fun toString(): String {
        return "Vilkår(type=$type)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vilkår

        if (type != other.type) return false
        if (vilkårsperioder != other.vilkårsperioder) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + vilkårsperioder.hashCode()
        return result
    }
}
