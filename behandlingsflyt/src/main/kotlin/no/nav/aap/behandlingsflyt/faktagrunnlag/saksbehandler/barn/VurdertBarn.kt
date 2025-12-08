package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid

sealed class BarnIdentifikator : Comparable<BarnIdentifikator> {
    abstract fun hentIdent(): Ident?

    data class BarnIdent(
        val ident: Ident,
        val navn: String? = null,
        val fødselsdato: Fødselsdato? = null
    ) : BarnIdentifikator() {
        constructor(ident: String) : this(Ident(ident))

        override fun hentIdent(): Ident = ident

        override fun compareTo(other: BarnIdentifikator): Int {
            return when (other) {
                is BarnIdent -> ident.identifikator.compareTo(other.ident.identifikator)
                is NavnOgFødselsdato -> -1 // Forskjellige typer, ikke sammenlignbare
            }
        }
    }

    data class NavnOgFødselsdato(val navn: String, val fødselsdato: Fødselsdato) : BarnIdentifikator() {
        override fun hentIdent(): Ident? = null

        override fun compareTo(other: BarnIdentifikator): Int {
            return when (other) {
                is BarnIdent -> 1 // Forskjellige typer, ikke sammenlignbare
                is NavnOgFødselsdato -> {
                    val navnComparison = navn.trim().compareTo(other.navn.trim(), ignoreCase = true)
                    if (navnComparison != 0) navnComparison else fødselsdato.toLocalDate()
                        .compareTo(other.fødselsdato.toLocalDate())
                }
            }
        }
    }

    fun er(other: BarnIdentifikator) = this == other || this.compareTo(other) == 0 || other.compareTo(this) == 0
}

data class VurdertBarn(val ident: BarnIdentifikator, val vurderinger: List<VurderingAvForeldreAnsvar>) {
    fun tilTidslinje(): Tidslinje<ForeldreansvarVurdering> {
        val til = when (ident) {
            is BarnIdentifikator.BarnIdent -> ident.fødselsdato?.let {
                Barn.periodeMedRettTil(it, null).tom
            } ?: Tid.MAKS
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
