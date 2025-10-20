package no.nav.aap.behandlingsflyt.behandling.lovvalg

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class MedlemskapLovvalgGrunnlag(
    val medlemskapArbeidInntektGrunnlag: MedlemskapArbeidInntektGrunnlag?,
    val personopplysning: Personopplysning?,
    val nyeSoknadGrunnlag: UtenlandsOppholdData?
) : Faktagrunnlag

data class MedlemskapArbeidInntektGrunnlag(
    val medlemskapGrunnlag: MedlemskapUnntakGrunnlag?,
    val inntekterINorgeGrunnlag: List<InntektINorgeGrunnlag>,
    val arbeiderINorgeGrunnlag: List<ArbeidINorgeGrunnlag>,

    @Deprecated("Ikke periodisert - skal fases ut")
    val manuellVurdering: ManuellVurderingForLovvalgMedlemskap?,

    val vurderinger: List<ManuellVurderingForLovvalgMedlemskap> = emptyList()
)

data class InntektINorgeGrunnlag(
    val identifikator: String,
    val beloep: Double,
    val skattemessigBosattLand: String?,
    val opptjeningsLand: String?,
    val inntektType: String?,
    val periode: Periode,
    val organisasjonsNavn: String?
)

data class ArbeidINorgeGrunnlag(
    val identifikator: String,
    val arbeidsforholdKode: String,
    val startdato: LocalDate,
    val sluttdato: LocalDate?
)

data class EnhetGrunnlag(
    val orgnummer: String,
    val orgNavn: String
)

enum class InntektTyper {
    SYKEPENGER,
    SYKEPENGERTILFISKERSOMBAREHARHYRE,
    SYKEPENGERTILDAGMAMMA,
    SYKEPENGERTILFISKER,
    SYKEPENGERTILJORDOGSKOGBRUKERE,
    FERIEPENGERSYKEPENGERTILFISKERSOMBAREHARHYRE,
}