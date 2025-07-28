package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnFraRegister
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid

sealed class BarnIdentifikator {
    data class RegistertBarnPerson(val personId: PersonId) : BarnIdentifikator()

    data class NavnOgFødselsdato(val navn: String, val fødselsdato: Fødselsdato) : BarnIdentifikator()

    data class BarnIdent(val ident: Ident) : BarnIdentifikator() {
        constructor(ident: String) : this(Ident(ident))

    }

    fun er(other: BarnIdentifikator) = this == other // || this.compareTo(other) == 0 || other.compareTo(this) == 0
}

data class VurdertBarn(val ident: BarnIdentifikator, val vurderinger: List<VurderingAvForeldreAnsvar>) {
    fun tilTidslinje(): Tidslinje<ForeldreansvarVurdering> {
        val til = when (ident) {
            is BarnIdentifikator.RegistertBarnPerson -> Tid.MAKS
            is BarnIdentifikator.NavnOgFødselsdato -> BarnFraRegister.periodeMedRettTil(ident.fødselsdato).tom
            is BarnIdentifikator.BarnIdent -> Tid.MAKS
        }
        return vurderinger.sortedBy { it.fraDato }.map {
            Tidslinje(
                Periode(it.fraDato, til),
                ForeldreansvarVurdering(it.harForeldreAnsvar, it.begrunnelse)
            )
        }.fold(Tidslinje<ForeldreansvarVurdering>()) { eksisterende, vurdering ->
            eksisterende.kombiner(vurdering, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }.komprimer()
    }

    data class ForeldreansvarVurdering(val harForeldreAnsvar: Boolean, val begrunnelse: String)
}
