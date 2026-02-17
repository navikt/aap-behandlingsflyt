package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak

sealed interface StansEllerOpphør {
    val årsaker: Set<Avslagsårsak>

    companion object {
        fun fraÅrsaker(årsaker: Set<Avslagsårsak>): StansEllerOpphør {
            return when {
                årsaker.any { it.avslagstype == Avslagstype.OPPHØR } -> Opphør(årsaker)
                årsaker.any { it.avslagstype == Avslagstype.STANS } -> Stans(årsaker)
                else ->
                    error("Trenger minst en avslagsårsak av type STANS eller OPPHØR, fikk $årsaker")
            }
        }
    }
}

data class Stans(
    override val årsaker: Set<Avslagsårsak>
): StansEllerOpphør

data class Opphør(
    override val årsaker: Set<Avslagsårsak>
): StansEllerOpphør
