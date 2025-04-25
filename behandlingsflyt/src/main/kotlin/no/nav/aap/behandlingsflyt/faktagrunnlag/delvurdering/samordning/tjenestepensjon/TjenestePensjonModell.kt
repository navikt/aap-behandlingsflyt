package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon

import java.time.LocalDate

data class TjenestePensjonForhold(
    val ordning: TjenestePensjonOrdning,
    val ytelser: List<TjenestePensjonYtelse>
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


// https://github.com/navikt/tp/blob/e99c670da41c23172e2ccc3a3e8dff4c7870fa82/tp-api/src/main/kotlin/no/nav/samhandling/tp/domain/codestable/YtelseTypeCode.kt#L6
enum class YtelseTypeCode(val isSamordningspliktigForAAP: Boolean) {
    ALDER(true),
    UFORE(true),
    GJENLEVENDE(true),
    BARN(true),
    AFP(true),
    UKJENT(true),
    OPPSATT_BTO_PEN(true),
    SAERALDER(true),
    PAASLAGSPENSJON(false),
    OVERGANGSTILLEGG(false),
    BETINGET_TP(false),
    LIVSVARIG_AFP(false);
}
