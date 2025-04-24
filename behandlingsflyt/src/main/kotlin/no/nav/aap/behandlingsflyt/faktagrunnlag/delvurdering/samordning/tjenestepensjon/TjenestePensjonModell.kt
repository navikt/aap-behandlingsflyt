package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon

import java.time.LocalDate
import java.time.LocalDateTime

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
    val datoInnmeldtYtelseFom: LocalDate?, // Nullable to handle null values
    val ytelseType: YtelseTypeCode,
    val datoYtelseIverksattFom: LocalDate,
    val datoYtelseIverksattTom: LocalDate?, // Nullable to handle null values
)


data class TjenestePensjonRespons(
    val fnr: String,
    val forhold: List<SamhandlerForholdDto> = emptyList(),
    val changeStamp: ChangeStampDateDto? = null
) {
    fun toIntern():List<TjenestePensjonForhold>{
        return this.forhold.map {
            TjenestePensjonForhold(
                ordning = TjenestePensjonOrdning(
                    navn = it.ordning.navn,
                    tpNr = it.ordning.tpNr,
                    orgNr = it.ordning.orgNr
                ),
                ytelser = it.ytelser.filter { it.ytelseType.isSamordningspliktigForAAP }.map { ytelse ->
                    TjenestePensjonYtelse(
                        datoInnmeldtYtelseFom = ytelse.datoInnmeldtYtelseFom,
                        ytelseType = ytelse.ytelseType,
                        datoYtelseIverksattFom = ytelse.datoYtelseIverksattFom,
                        datoYtelseIverksattTom = ytelse.datoYtelseIverksattTom
                    )
                }
            )
        }
    }
}

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
