package no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap

data class ManuellVurderingForForutgåendeMedlemskap (
    val begrunnelse: String,
    val harForutgåendeMedlemskap: Boolean,
    val oppfyllerUnntaksVilkår: Boolean?
)