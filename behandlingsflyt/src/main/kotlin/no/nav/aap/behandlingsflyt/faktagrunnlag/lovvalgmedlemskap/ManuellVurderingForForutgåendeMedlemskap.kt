package no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap

import java.time.LocalDate

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

data class HistoriskManuellVurderingForForutgåendeMedlemskap(
    val manuellVurdering: ManuellVurderingForForutgåendeMedlemskap,
    val opprettet: LocalDate
)