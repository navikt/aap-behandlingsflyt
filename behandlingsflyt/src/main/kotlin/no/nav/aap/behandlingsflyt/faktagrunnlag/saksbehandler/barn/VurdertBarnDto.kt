package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import java.time.LocalDate

data class VurderingAvForeldreAnsvarDto(
    val fraDato: LocalDate,
    val harForeldreAnsvar: Boolean,
    val begrunnelse: String
) {
    fun tilVurderingAvForeldreAnsvar() = VurderingAvForeldreAnsvar(fraDato, harForeldreAnsvar, begrunnelse)
}

open class VurdertBarnDto(
    val ident: String?,
    val navn: String?,
    val fødselsdato: LocalDate?,
    val vurderinger: List<VurderingAvForeldreAnsvarDto>
) {
    init {
        if (ident == null) {
            requireNotNull(navn)
            requireNotNull(fødselsdato)
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