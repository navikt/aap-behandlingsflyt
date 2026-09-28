package no.nav.aap.behandlingsflyt.behandling.lovvalg

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikkGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.komponenter.tidslinje.Tidslinje

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
    fun gjeldendeVurderinger(): Tidslinje<ManuellVurderingForForutgåendeMedlemskap> {
        return vurderinger.gjeldendeVurderinger()
    }
}