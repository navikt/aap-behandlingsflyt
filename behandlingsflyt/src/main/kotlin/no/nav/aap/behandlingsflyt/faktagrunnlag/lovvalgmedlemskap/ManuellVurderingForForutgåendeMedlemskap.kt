package no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
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
    val vurdertIBehandling: BehandlingId,
    val fom: LocalDate,
    val tom: LocalDate? = null
) {
    // NB! Denne tar ikke høyde for yrkesskade
    fun oppfyllerForutgåendeMedlemskap(): Boolean {
        return harForutgåendeMedlemskap
                || varMedlemMedNedsattArbeidsevne == true
                || medlemMedUnntakAvMaksFemAar == true
    }
}

data class ManuellVurderingForForutgåendeMedlemskapDto(
    val begrunnelse: String,
    val harForutgåendeMedlemskap: Boolean,
    val varMedlemMedNedsattArbeidsevne: Boolean?,
    val medlemMedUnntakAvMaksFemAar: Boolean?
)

data class PeriodisertManuellVurderingForForutgåendeMedlemskapDto(
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val begrunnelse: String,
    val harForutgåendeMedlemskap: Boolean,
    val varMedlemMedNedsattArbeidsevne: Boolean?,
    val medlemMedUnntakAvMaksFemAar: Boolean?
) : LøsningForPeriode {
    fun toManuellVurderingForForutgåendeMedlemskap(
        kontekst: AvklaringsbehovKontekst,
        overstyrt : Boolean,
    ): ManuellVurderingForForutgåendeMedlemskap = ManuellVurderingForForutgåendeMedlemskap(
        fom = fom,
        tom = tom,
        vurdertIBehandling = kontekst.behandlingId(),
        begrunnelse = begrunnelse,
        harForutgåendeMedlemskap = harForutgåendeMedlemskap,
        varMedlemMedNedsattArbeidsevne = varMedlemMedNedsattArbeidsevne,
        medlemMedUnntakAvMaksFemAar = medlemMedUnntakAvMaksFemAar,
        vurdertAv = kontekst.bruker.ident,
        vurdertTidspunkt = LocalDateTime.now(),
        overstyrt = overstyrt
    )
}

data class HistoriskManuellVurderingForForutgåendeMedlemskap(
    val manuellVurdering: ManuellVurderingForForutgåendeMedlemskap,
    val opprettet: LocalDateTime,
    val erGjeldendeVurdering: Boolean
)