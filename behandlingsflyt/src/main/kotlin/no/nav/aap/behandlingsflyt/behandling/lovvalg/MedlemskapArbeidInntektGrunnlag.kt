package no.nav.aap.behandlingsflyt.behandling.lovvalg

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.utils.Validation
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
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
    val vurderinger: List<ManuellVurderingForLovvalgMedlemskap> = emptyList()
) {
    fun gjeldendeVurderinger(maksDato: LocalDate = Tid.MAKS): Tidslinje<ManuellVurderingForLovvalgMedlemskap> {
        return vurderinger.tilTidslinje(maksDato)
    }
}

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

fun List<ManuellVurderingForLovvalgMedlemskap>.tilTidslinje(maksDato: LocalDate = Tid.MAKS): Tidslinje<ManuellVurderingForLovvalgMedlemskap> =
    sortedBy { it.vurdertDato }.somTidslinje { Periode(it.fom, it.tom ?: maksDato) }

fun Tidslinje<ManuellVurderingForLovvalgMedlemskap>.validerGyldigForRettighetsperiode(rettighetsperiode: Periode): Validation<Tidslinje<ManuellVurderingForLovvalgMedlemskap>> {
    val periodeForVurdering = helePerioden()

    if (!erSammenhengende()) {
        return Validation.Invalid(this, "Periodene for lovvalg og medlemskap er ikke sammenhengende")
    }

    if(periodeForVurdering.fom > rettighetsperiode.fom) {
        return Validation.Invalid(this, "Det er ikke tatt stilling til hele rettighetsperioden. Rettighetsperioden for saken starter ${rettighetsperiode.fom} mens vurderingens første periode starter ${periodeForVurdering.fom}. ")
    }

    if(periodeForVurdering.tom < rettighetsperiode.tom) {
        return Validation.Invalid(this, "Det er ikke tatt stilling til hele rettighetsperioden. Rettighetsperioden for saken slutter ${rettighetsperiode.tom} mens vurderingens siste periode slutter ${periodeForVurdering.tom}. ")
    }

    return Validation.Valid(this)
}
