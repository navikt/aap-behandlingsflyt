package no.nav.aap.behandlingsflyt.behandling.lovvalg

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidVurdering
import no.nav.aap.behandlingsflyt.utils.Validation
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
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

    @Deprecated("Ikke periodisert - skal fases ut")
    val manuellVurdering: ManuellVurderingForLovvalgMedlemskap?,

    val vurderinger: List<ManuellVurderingForLovvalgMedlemskap> = emptyList()
) {
    fun gjeldendeVurderinger(maksDato: LocalDate = Tid.MAKS): Tidslinje<ManuellVurderingForLovvalgMedlemskap> {
        return vurderinger
            .groupBy { it.vurdertIBehandling }
            .values
            .sortedBy { it[0].vurdertDato }
            .flatMap { it.sortedBy { it.fom } }
            .somTidslinje { Periode(it.fom!!, it.tom ?: maksDato) }
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

fun Collection<ManuellVurderingForLovvalgMedlemskap>.tilTidslinje(): Tidslinje<ManuellVurderingForLovvalgMedlemskap> =
    sortedBy { it.vurdertDato }
        .groupBy { it.vurdertIBehandling }
        .map {
            val vurderingerForBehandling = it.value.sortedBy { vurdering -> vurdering.fom }
            Tidslinje(
                vurderingerForBehandling.map { periode ->
                    Segment(
                        // TODO etter eksisterende data er migrert, vil ikke lenger fom være nullable
                        periode = Periode(fom = periode.fom!!, tom = periode.tom ?: LocalDate.MAX),
                        verdi = periode
                    )
                }
            )
        }
        .fold(Tidslinje()) { acc, other -> acc.kombiner(other, StandardSammenslåere.prioriterHøyreSideCrossJoin()) }

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
