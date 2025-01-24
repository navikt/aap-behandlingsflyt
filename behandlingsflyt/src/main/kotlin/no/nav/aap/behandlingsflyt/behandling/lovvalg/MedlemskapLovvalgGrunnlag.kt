package no.nav.aap.behandlingsflyt.behandling.lovvalg

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class MedlemskapLovvalgGrunnlag(
    val medlemskapGrunnlag: MedlemskapUnntakGrunnlag?,
    val inntekterINorgeGrunnlag : List<InntektINorgeGrunnlag>,
    val arbeiderINorgeGrunnlag : List<ArbeidINorgeGrunnlag>
)

data class InntektINorgeGrunnlag(
    val identifikator: String,
    val beloep: Double,
    val skattemessigBosattLand: String?,
    val opptjeningsLand: String?,
    val inntektType: String?,
    val periode: Periode,
)

data class ArbeidINorgeGrunnlag(
    val identifikator: String,
    val arbeidsforholdKode: String,
    val startdato: LocalDate,
    val sluttdato: LocalDate?
)

enum class InntektTyper {
    SYKEPENGER,
    SYKEPENGERTILFISKERSOMBAREHARHYRE
}