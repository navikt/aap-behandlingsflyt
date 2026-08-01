package no.nav.aap.lovvalg

import java.time.LocalDate
import no.nav.aap.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.personopplysninger.PersonopplysningMedHistorikkGrunnlag
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.misc.Faktagrunnlag

data class ForutgåendeMedlemskapGrunnlag(
    val medlemskapArbeidInntektGrunnlag: ForutgåendeMedlemskapArbeidInntektGrunnlag?,
    val personopplysningGrunnlag: PersonopplysningMedHistorikkGrunnlag?,
    val nyeSoknadGrunnlag: UtenlandsOppholdData?
) : Faktagrunnlag

data class ForutgåendeMedlemskapArbeidInntektGrunnlag(
    val medlemskapGrunnlag: MedlemskapUnntakGrunnlag?,
    val inntekterINorgeGrunnlag: List<InntektINorgeGrunnlag>,
    val arbeiderINorgeGrunnlag: List<ArbeidINorgeGrunnlag>,
    val vurderinger: List<ManuellVurderingForForutgåendeMedlemskap>,
) {
    fun gjeldendeVurderinger(maksDato: LocalDate = Tid.MAKS): Tidslinje<ManuellVurderingForForutgåendeMedlemskap> {
        return vurderinger.tilTidslinje(maksDato)
    }
}

fun List<ManuellVurderingForForutgåendeMedlemskap>.tilTidslinje(maksDato: LocalDate = Tid.MAKS): Tidslinje<ManuellVurderingForForutgåendeMedlemskap> =
    sortedBy { it.vurdertTidspunkt }
        .somTidslinje { Periode(it.fom, it.tom ?: Tid.MAKS) }
        .komprimer()
        .begrensetTil(Periode(Tid.MIN, maksDato))