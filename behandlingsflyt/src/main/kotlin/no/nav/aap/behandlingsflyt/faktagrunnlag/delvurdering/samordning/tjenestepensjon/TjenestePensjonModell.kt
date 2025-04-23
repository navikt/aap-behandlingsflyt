package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon

import java.time.LocalDate
import java.time.LocalDateTime

data class TjenestePensjon(
    val tpNr: List<String>
)

data class TjenestePensonRespons(
    val fnr: String,
    val forhold: List<SamhandlerForholdDto> = emptyList(),
    val changeStamp: ChangeStampDateDto? = null
)

data class SamhandlerForholdDto(
    val samtykkeSimulering: Boolean,
    val kilde: String,
    val tpNr: String,
    val ordning: TpOrdning,
    val harSimulering: Boolean,
    val harUtlandsPensjon: Boolean,
    val datoSamtykkeGitt: LocalDate?, // Nullable to handle null values
    val ytelser: List<SamhandlerYtelseDto>,
    val changeStampDate: ChangeStampDateDto?
)

data class TpOrdning(
    val navn: String,
    val tpNr: String,
    val orgNr: String,
    val tssId: String,
    val alias: List<String>
)

data class SamhandlerYtelseDto(
    val datoInnmeldtYtelseFom: LocalDate?, // Nullable to handle null values
    val ytelseType: YtelseTypeCode,
    val datoYtelseIverksattFom: LocalDate,
    val datoYtelseIverksattTom: LocalDate?, // Nullable to handle null values
    val changeStamp: ChangeStampDateDto?,
    val ytelseId: Long
)

data class ChangeStampDateDto(
    val createdBy: String,
    val createdDate: LocalDateTime,
    val updatedBy: String,
    val updatedDate: LocalDateTime
)

enum class YtelseTypeCode(val isSamordningspliktig: Boolean) {
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
