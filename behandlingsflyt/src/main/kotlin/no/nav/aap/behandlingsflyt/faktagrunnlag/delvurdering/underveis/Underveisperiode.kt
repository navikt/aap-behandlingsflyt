package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import java.util.*

data class Underveisperiode(
    val periode: Periode,
    val meldePeriode: Periode,
    val utfall: Utfall,
    val avslagsårsak: UnderveisÅrsak?,
    val grenseverdi: Prosent,
    val gradering: Gradering,
    val trekk: Dagsatser,
    val kvoterBrukt: Set<Kvote>,
) : Comparable<Underveisperiode> {

    fun utbetalingsgrad(): Prosent {
        if (utfall == Utfall.IKKE_OPPFYLT) {
            return Prosent.`0_PROSENT`
        }
        return gradering.gradering
    }

    override fun compareTo(other: Underveisperiode): Int {
        return periode.compareTo(other.periode)
    }


    override fun toString(): String {
        return "Underveisperiode(periode=$periode, utfall=$utfall, utbetalingsgrad=${utbetalingsgrad()}, avslagsårsak=$avslagsårsak, grenseverdi=$grenseverdi, gradering=$gradering, trekk=$trekk)"
    }

}