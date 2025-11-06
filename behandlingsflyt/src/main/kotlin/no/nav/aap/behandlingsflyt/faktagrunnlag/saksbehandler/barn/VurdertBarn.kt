package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid

sealed class BarnIdentifikator : Comparable<BarnIdentifikator> {
    data class BarnIdent(val ident: Ident) : BarnIdentifikator() {
        constructor(ident: String) : this(Ident(ident))

        override fun compareTo(other: BarnIdentifikator): Int {
            return when (other) {
                is BarnIdent -> other.ident.identifikator.compareTo(ident.identifikator)
                is NavnOgFødselsdato -> other.compareTo(this)
            }
        }
    }

    data class NavnOgFødselsdato(val navn: String, val fødselsdato: Fødselsdato) : BarnIdentifikator() {
        override fun compareTo(other: BarnIdentifikator): Int {
            return when (other) {
                is BarnIdent -> other.ident.identifikator.compareTo(navn)
                is NavnOgFødselsdato -> other.navn.compareTo(this.navn)
            }
        }
    }

    fun er(other: BarnIdentifikator) = this == other || this.compareTo(other) == 0 || other.compareTo(this) == 0
}

data class VurdertBarn(val ident: BarnIdentifikator, val vurderinger: List<VurderingAvForeldreAnsvar>) {
    fun tilTidslinje(): Tidslinje<ForeldreansvarVurdering> {
        val til = when (ident) {
            is BarnIdentifikator.BarnIdent -> Tid.MAKS
            is BarnIdentifikator.NavnOgFødselsdato -> Barn.periodeMedRettTil(ident.fødselsdato, null).tom
        }
        return vurderinger.sortedBy { it.fraDato }.map {
            Tidslinje(
                Periode(it.fraDato, til),
                ForeldreansvarVurdering(it.harForeldreAnsvar, it.begrunnelse, it.erFosterForelder)
            )
        }.fold(Tidslinje<ForeldreansvarVurdering>()) { eksisterende, vurdering ->
            eksisterende.kombiner(vurdering, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }.komprimer()
    }

    data class ForeldreansvarVurdering(val harForeldreAnsvar: Boolean, val begrunnelse: String, val erFosterforelder: Boolean? = null)
}
