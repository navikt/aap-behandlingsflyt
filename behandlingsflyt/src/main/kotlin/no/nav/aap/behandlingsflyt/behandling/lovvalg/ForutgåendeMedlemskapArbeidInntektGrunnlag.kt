package no.nav.aap.behandlingsflyt.behandling.lovvalg

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikkGrunnlag

data class ForutgåendeMedlemskapGrunnlag(
    val medlemskapArbeidInntektGrunnlag: ForutgåendeMedlemskapArbeidInntektGrunnlag?,
    val personopplysningGrunnlag: PersonopplysningMedHistorikkGrunnlag?,
    var nyeSoknadGrunnlag: UtenlandsOppholdData?
) : Faktagrunnlag

data class ForutgåendeMedlemskapArbeidInntektGrunnlag(
    val medlemskapGrunnlag: MedlemskapUnntakGrunnlag?,
    val inntekterINorgeGrunnlag: List<InntektINorgeGrunnlag>,
    val arbeiderINorgeGrunnlag: List<ArbeidINorgeGrunnlag>,
    private val manuellVurdering_: ManuellVurderingForForutgåendeMedlemskap?,
    val vurderinger: List<ManuellVurderingForForutgåendeMedlemskap>,
) {
    // TODO midlertidig for å støtte nye verdier som henter fra vurderinger og gamle verdier som ikke ennå har en kobling
    val manuellVurdering: ManuellVurderingForForutgåendeMedlemskap?
        get() = vurderinger.firstOrNull() ?: manuellVurdering_
}