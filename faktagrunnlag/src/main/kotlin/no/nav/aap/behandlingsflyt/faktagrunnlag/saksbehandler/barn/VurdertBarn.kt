package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.verdityper.sakogbehandling.Ident

class VurdertBarn(val ident: Ident, val vurderinger: List<VurderingAvForeldreAnsvar>) {
    fun tilTidslinje(): Tidslinje<ForeldreansvarVurdering> {
        return vurderinger.sortedBy { it.fraDato }.map {
            Tidslinje(
                Periode(it.fraDato, Tid.MAKS),
                ForeldreansvarVurdering(it.harForeldreAnsvar, it.begrunnelse)
            )
        }.fold(Tidslinje<ForeldreansvarVurdering>()) { eksisterende, vurdering ->
            eksisterende.kombiner(vurdering, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }.komprimer()
    }

    data class ForeldreansvarVurdering(val harForeldreAnsvar: Boolean, val begrunnelse: String)
}
