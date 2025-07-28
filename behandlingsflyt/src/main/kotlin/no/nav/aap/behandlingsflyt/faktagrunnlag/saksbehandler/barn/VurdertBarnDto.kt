package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn

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

}