package no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap

import java.time.LocalDateTime

data class ManuellVurderingForForutgåendeMedlemskap(
    val begrunnelse: String,
    val harForutgåendeMedlemskap: Boolean,
    val varMedlemMedNedsattArbeidsevne: Boolean?,
    val medlemMedUnntakAvMaksFemAar: Boolean?,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime? = null,
    val overstyrt: Boolean = false
)

data class ManuellVurderingForForutgåendeMedlemskapDto(
    val begrunnelse: String,
    val harForutgåendeMedlemskap: Boolean,
    val varMedlemMedNedsattArbeidsevne: Boolean?,
    val medlemMedUnntakAvMaksFemAar: Boolean?
)

data class HistoriskManuellVurderingForForutgåendeMedlemskap(
    val manuellVurdering: ManuellVurderingForForutgåendeMedlemskap,
    val opprettet: LocalDateTime
)