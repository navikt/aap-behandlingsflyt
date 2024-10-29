package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.Dagsatser
import no.nav.aap.verdityper.Prosent
import java.util.*

class Underveisperiode(
    val periode: Periode,
    val meldePeriode: Periode?,
    val utfall: Utfall,
    val avslagsårsak: UnderveisÅrsak?,
    val grenseverdi: Prosent,
    val gradering: Gradering?,
    val trekk: Dagsatser
) : Comparable<Underveisperiode> {

    fun utbetalingsgrad(): Prosent {
        if (utfall == Utfall.IKKE_OPPFYLT) {
            return Prosent.`0_PROSENT`
        }
        return gradering?.gradering ?: Prosent.`0_PROSENT`
    }

    override fun compareTo(other: Underveisperiode): Int {
        return periode.compareTo(other.periode)
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Underveisperiode

        if (periode != other.periode) return false
        if (meldePeriode != other.meldePeriode) return false
        if (utfall != other.utfall) return false
        if (avslagsårsak != other.avslagsårsak) return false
        if (grenseverdi != other.grenseverdi) return false
        if (gradering != other.gradering) return false
        if (trekk != other.trekk) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(periode, meldePeriode, utfall, avslagsårsak, grenseverdi, gradering, trekk)
    }

    override fun toString(): String {
        return "Underveisperiode(periode=$periode, utfall=$utfall, utbetalingsgrad=${utbetalingsgrad()}, avslagsårsak=$avslagsårsak, grenseverdi=$grenseverdi, gradering=$gradering, trekk=$trekk)"
    }

}