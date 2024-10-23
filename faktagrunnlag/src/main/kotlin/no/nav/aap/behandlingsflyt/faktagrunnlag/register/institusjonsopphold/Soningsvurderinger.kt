package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Tid

data class Soningsvurderinger(internal val id: Long? = null, val vurderinger: List<Soningsvurdering>) {
    fun tilTidslinje(): Tidslinje<Soningsvurdering> {
        return vurderinger.sortedBy { it.fraDato }.map { Tidslinje(Periode(it.fraDato, Tid.MAKS), it) }
            .fold(Tidslinje<Soningsvurdering>()) { acc, tidslinje ->
                acc.kombiner(tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }
    }
}