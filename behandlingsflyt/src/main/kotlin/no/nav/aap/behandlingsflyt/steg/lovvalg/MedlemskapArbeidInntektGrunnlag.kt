package no.nav.aap.behandlingsflyt.steg.lovvalg

import java.time.LocalDate
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.lovvalg.InntektINorgeGrunnlag
import no.nav.aap.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.misc.Validation
import no.nav.aap.personopplysninger.Personopplysning

data class MedlemskapLovvalgGrunnlag(
    val medlemskapArbeidInntektGrunnlag: MedlemskapArbeidInntektGrunnlag?,
    val personopplysning: Personopplysning?,
    val nyeSoknadGrunnlag: UtenlandsOppholdData?
) : Faktagrunnlag

data class MedlemskapArbeidInntektGrunnlag(
    val medlemskapGrunnlag: MedlemskapUnntakGrunnlag?,
    val inntekterINorgeGrunnlag: List<InntektINorgeGrunnlag>,
    val arbeiderINorgeGrunnlag: List<ArbeidINorgeGrunnlag>,
    val vurderinger: List<ManuellVurderingForLovvalgMedlemskap> = emptyList()
) {
    fun gjeldendeVurderinger(maksDato: LocalDate = Tid.MAKS): Tidslinje<ManuellVurderingForLovvalgMedlemskap> {
        return vurderinger.tilTidslinje(maksDato)
    }
}

fun List<PeriodisertManuellVurderingForLovvalgMedlemskapDto>.validerGyldigVurderinger(): Validation<List<PeriodisertManuellVurderingForLovvalgMedlemskapDto>> {
    forEach {
        val periode = if (it.tom != null) "${it.fom} - ${it.tom}" else "${it.fom}"
        if (it.lovvalg.begrunnelse.isBlank()) {
            return Validation.Invalid(this, "Det mangler begrunnelse for lovvalg [$periode]")
        }
        if (it.medlemskap != null && it.medlemskap.begrunnelse.isBlank()) {
            return Validation.Invalid(this, "Det mangler begrunnelse for medlemskap [$periode]")
        }
    }

    return Validation.Valid(this)
}