package no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate
import java.time.LocalDateTime

data class ManuellVurderingForForutgåendeMedlemskap(
    val id: Long? = null,
    val begrunnelse: String,
    val harForutgåendeMedlemskap: Boolean,
    val varMedlemMedNedsattArbeidsevne: Boolean?,
    val medlemMedUnntakAvMaksFemAar: Boolean?,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime,
    val overstyrt: Boolean = false,

    // TODO disse skal ikke være nullable etter migrering
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
    val vurdertIBehandling: BehandlingId? = null,
)

data class ManuellVurderingForForutgåendeMedlemskapDto(
    val begrunnelse: String,
    val harForutgåendeMedlemskap: Boolean,
    val varMedlemMedNedsattArbeidsevne: Boolean?,
    val medlemMedUnntakAvMaksFemAar: Boolean?
)

data class HistoriskManuellVurderingForForutgåendeMedlemskap(
    val manuellVurdering: ManuellVurderingForForutgåendeMedlemskap,
    val opprettet: LocalDateTime,
    val erGjeldendeVurdering: Boolean
)