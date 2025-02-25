package no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap

data class ManuellVurderingForForutgåendeMedlemskap (
    val begrunnelse: String,
    val harForutgåendeMedlemskap: Boolean,
    val varMedlemMedNedsattArbeidsevne: Boolean?,
    val medlemMedUnntakAvMaksFemAar: Boolean?,
    val overstyrt: Boolean = false
)

data class ManuellVurderingForForutgåendeMedlemskapDto (
    val begrunnelse: String,
    val harForutgåendeMedlemskap: Boolean,
    val varMedlemMedNedsattArbeidsevne: Boolean?,
    val medlemMedUnntakAvMaksFemAar: Boolean?,
)