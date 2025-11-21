package no.nav.aap.behandlingsflyt.behandling.lovvalg

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikkGrunnlag
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class ForutgåendeMedlemskapGrunnlag(
    val medlemskapArbeidInntektGrunnlag: ForutgåendeMedlemskapArbeidInntektGrunnlag?,
    val personopplysningGrunnlag: PersonopplysningMedHistorikkGrunnlag?,
    var nyeSoknadGrunnlag: UtenlandsOppholdData?
) : Faktagrunnlag

data class ForutgåendeMedlemskapArbeidInntektGrunnlag(
    val medlemskapGrunnlag: MedlemskapUnntakGrunnlag?,
    val inntekterINorgeGrunnlag: List<InntektINorgeGrunnlag>,
    val arbeiderINorgeGrunnlag: List<ArbeidINorgeGrunnlag>,
    val manuellVurdering: ManuellVurderingForForutgåendeMedlemskap?,
    val vurderinger: List<ManuellVurderingForForutgåendeMedlemskap>,
) {
    fun gjeldendeVurderinger(maksDato: LocalDate = Tid.MAKS): Tidslinje<ManuellVurderingForForutgåendeMedlemskap> {
        return vurderinger.tilTidslinje(maksDato)
    }
}

fun List<ManuellVurderingForForutgåendeMedlemskap>.tilTidslinje(maksDato: LocalDate = Tid.MAKS): Tidslinje<ManuellVurderingForForutgåendeMedlemskap> =
    // TODO fjern !! når migrering er fullført
    sortedBy { it.vurdertTidspunkt }.somTidslinje { Periode(it.fom!!, it.tom ?: maksDato) }