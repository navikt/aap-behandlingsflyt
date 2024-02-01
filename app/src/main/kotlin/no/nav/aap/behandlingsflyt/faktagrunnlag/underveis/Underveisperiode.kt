package no.nav.aap.behandlingsflyt.faktagrunnlag.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.underveis.regler.Gradering
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.Prosent

class Underveisperiode(
    val periode: Periode,
    val utfall: Utfall,
    val avslagsårsak: UnderveisAvslagsårsak?,
    val grenseverdi: Prosent,
    val gradering: Gradering?
) : Comparable<Underveisperiode> {

    fun utbetalingsgrad(): Prosent {
        if(utfall == Utfall.IKKE_OPPFYLT) {
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
        if (utfall != other.utfall) return false
        if (avslagsårsak != other.avslagsårsak) return false
        if (grenseverdi != other.grenseverdi) return false
        if (gradering != other.gradering) return false

        return true
    }

    override fun hashCode(): Int {
        var result = periode.hashCode()
        result = 31 * result + utfall.hashCode()
        result = 31 * result + (avslagsårsak?.hashCode() ?: 0)
        result = 31 * result + grenseverdi.hashCode()
        result = 31 * result + gradering.hashCode()
        return result
    }

    override fun toString(): String {
        return "Underveisperiode(periode=$periode, utfall=$utfall, utbetalingsgrad=${utbetalingsgrad()}, avslagsårsak=$avslagsårsak, grenseverdi=$grenseverdi, gradering=$gradering)"
    }

}