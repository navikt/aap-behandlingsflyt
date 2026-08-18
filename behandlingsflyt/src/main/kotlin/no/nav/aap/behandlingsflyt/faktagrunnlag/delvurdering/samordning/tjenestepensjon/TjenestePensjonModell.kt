package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon

import java.time.LocalDate

data class TjenestePensjonForhold(
    val ordning: TjenestePensjonOrdning,
    val ytelser: Set<TjenestePensjonYtelse>
)

data class TjenestePensjonOrdning(
    val navn: String,
    val tpNr: String,
    val orgNr: String,
)

data class TjenestePensjonYtelse(
    val innmeldtYtelseFom: LocalDate?, // Nullable to handle null values
    val ytelseType: YtelseTypeCode,
    val ytelseIverksattFom: LocalDate,
    val ytelseIverksattTom: LocalDate?, // Nullable to handle null values
    val ytelseId: Long,
)


// https://github.com/navikt/tp/blob/main/src/main/kotlin/no/nav/samhandling/tp/domain/codestable/YtelseTypeCode.kt
enum class YtelseTypeCode(val isSamordningspliktigForAAP: Boolean) {
    ALDER(true),
    UFORE(true),
    GJENLEVENDE(true),
    BARN(true),
    AFP(true),
    UKJENT(true),
    OPPSATT_BTO_PEN(true),
    SAERALDER(true),
    TIDLIGPENSJON(true),
    TIDLIGPEN_OVERGANG(true),
    SAERALDERSPAASLAG(false),
    PAASLAGSPENSJON(false),
    OVERGANGSTILLEGG(false),
    BETINGET_TP(false),
    LIVSVARIG_AFP(false);
}
