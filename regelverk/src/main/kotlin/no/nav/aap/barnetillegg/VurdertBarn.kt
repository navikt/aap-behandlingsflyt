package no.nav.aap.barnetillegg

import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid

data class VurdertBarn(val ident: BarnIdentifikator, val vurderinger: List<VurderingAvForeldreAnsvar>) {
    fun tilTidslinje(): Tidslinje<ForeldreansvarVurdering> {
        val til = when (ident) {
            is BarnIdentifikator.BarnIdent -> ident.fødselsdato?.let {
                Barn.periodeMedRettTil(it, null).tom
            } ?: Tid.MAKS

            is BarnIdentifikator.NavnOgFødselsdato -> Barn.periodeMedRettTil(ident.fødselsdato, null).tom
        }
        return vurderinger.sortedBy { it.fraDato }
            .filter { it.fraDato <= til }
            .map {
                Tidslinje(
                    Periode(it.fraDato, til),
                    ForeldreansvarVurdering(it.harForeldreAnsvar, it.begrunnelse, it.erFosterForelder)
                )
            }.fold(Tidslinje<ForeldreansvarVurdering>()) { eksisterende, vurdering ->
                eksisterende.kombiner(vurdering, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }.komprimer()
    }

    data class ForeldreansvarVurdering(
        val harForeldreAnsvar: Boolean,
        val begrunnelse: String,
        val erFosterforelder: Boolean? = null
    )
}