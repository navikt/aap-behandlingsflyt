package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning

import no.nav.aap.behandlingsflyt.behandling.lovvalg.EnhetGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektINorgeGrunnlag
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.YearMonth

data class InntektskomponentData(
    val arbeidsInntektMåned: List<ArbeidsInntektMåned> = emptyList()
)

data class ArbeidsInntektMåned(
    val årMåned: YearMonth,
    val arbeidsInntektInformasjon: ArbeidsInntektInformasjon
) {
    fun tilDomene(enhetGrunnlag: List<EnhetGrunnlag>): List<InntektINorgeGrunnlag> {
        return arbeidsInntektInformasjon.inntektListe.map { inntekt ->
            val orgNavn = enhetGrunnlag.firstOrNull { it.orgnummer == inntekt.virksomhet.identifikator }?.orgNavn
            val tomFallback = inntekt.opptjeningsperiodeFom ?: årMåned.atDay(1)

            InntektINorgeGrunnlag(
                identifikator = inntekt.virksomhet.identifikator,
                periode = Periode(
                    inntekt.opptjeningsperiodeFom ?: årMåned.atDay(1),
                    inntekt.opptjeningsperiodeTom ?: tomFallback
                ),
                organisasjonsNavn = orgNavn,
                beloep = inntekt.beløp,
                skattemessigBosattLand = inntekt.skattemessigBosattLand,
                opptjeningsLand = inntekt.opptjeningsland,
                inntektType = inntekt.beskrivelse,
            )
        }
    }
}

fun List<ArbeidsInntektMåned>.tilDomene(enhetGrunnlag: List<EnhetGrunnlag>): List<InntektINorgeGrunnlag> {
    return this.flatMap { it.tilDomene(enhetGrunnlag) }
}

data class ArbeidsInntektInformasjon(
    val inntektListe: List<Inntekt>
)

data class Inntekt(
    val beløp: Double,
    val opptjeningsland: String?,
    val skattemessigBosattLand: String?,
    val opptjeningsperiodeFom: LocalDate?,
    val opptjeningsperiodeTom: LocalDate?,
    val virksomhet: Virksomhet,
    val beskrivelse: String?
)

data class Virksomhet(
    val identifikator: String,
)