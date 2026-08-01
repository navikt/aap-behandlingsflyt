package no.nav.aap.behandlingsflyt.steg.medlemskap

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.misc.Validation

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
        vurdertAv = kontekst.bruker,
        vurdertTidspunkt = LocalDateTime.now(),
        overstyrt = overstyrt
    )
}

fun List<PeriodisertManuellVurderingForForutgåendeMedlemskapDto>.validerGyldigVurderinger(): Validation<List<PeriodisertManuellVurderingForForutgåendeMedlemskapDto>> {
    forEach {
        val periode = if (it.tom != null) "${it.fom} - ${it.tom}" else "${it.fom}"
        if (it.begrunnelse.isBlank()) {
            return Validation.Invalid(this, "Det mangler begrunnelse for vurdering [$periode]")
        }
        if (it.harForutgåendeMedlemskap && (it.medlemMedUnntakAvMaksFemAar == true || it.varMedlemMedNedsattArbeidsevne == true)) {
            return Validation.Invalid(this, "Kan ikke oppfylle både hovedvilår og unntaksvilkår [$periode]")
        }
    }

    return Validation.Valid(this)
}