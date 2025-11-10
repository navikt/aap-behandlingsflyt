package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Relasjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import java.time.LocalDate

data class VurderingAvForeldreAnsvarDto(
    val fraDato: LocalDate,
    val harForeldreAnsvar: Boolean,
    val begrunnelse: String,
    val erFosterForelder: Boolean? = null,
) {
    fun tilVurderingAvForeldreAnsvar() = VurderingAvForeldreAnsvar(
        fraDato = fraDato,
        harForeldreAnsvar = harForeldreAnsvar,
        begrunnelse = begrunnelse,
        erFosterForelder = erFosterForelder)
}

open class VurdertBarnDto(
    val ident: String?,
    val navn: String?,
    val fødselsdato: LocalDate?,
    val vurderinger: List<VurderingAvForeldreAnsvarDto>,
    val oppgittForeldreRelasjon: Relasjon? = null,
) {
    init {
        if (ident == null) {
            requireNotNull(navn) { "Om ident er null, krever navn. Fødselsdato: $fødselsdato." }
            requireNotNull(fødselsdato) { "Om ident er null, kreves fødselsdato. Navn: $navn." }
        }
    }

    fun toVurdertBarn(): VurdertBarn {
        val identifikator = if (ident == null) {
            BarnIdentifikator.NavnOgFødselsdato(navn!!, Fødselsdato(fødselsdato!!))
        } else {
            BarnIdentifikator.BarnIdent(Ident(ident))
        }
        return VurdertBarn(
            identifikator,
            vurderinger.map { it.tilVurderingAvForeldreAnsvar() })
    }
}