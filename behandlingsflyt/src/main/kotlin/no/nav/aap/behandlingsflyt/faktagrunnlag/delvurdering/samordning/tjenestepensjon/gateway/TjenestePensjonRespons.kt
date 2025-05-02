package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonForhold
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonOrdning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.YtelseTypeCode
import java.time.LocalDate

data class TjenestePensjonRespons(
    val fnr: String,
    val forhold: List<SamhandlerForholdDto> = emptyList()
) {
    fun toIntern(): List<TjenestePensjonForhold> {
        return this.forhold.map {
            TjenestePensjonForhold(
                ordning = TjenestePensjonOrdning(
                    navn = it.ordning.navn,
                    tpNr = it.ordning.tpNr,
                    orgNr = it.ordning.orgNr
                ),
                ytelser = it.ytelser.filter { it.ytelseType.isSamordningspliktigForAAP }.map { ytelse ->
                    TjenestePensjonYtelse(
                        innmeldtYtelseFom = ytelse.datoInnmeldtYtelseFom,
                        ytelseType = ytelse.ytelseType,
                        ytelseIverksattFom = ytelse.datoYtelseIverksattFom,
                        ytelseIverksattTom = ytelse.datoYtelseIverksattTom,
                        ytelseId = ytelse.ytelseId
                    )
                }
            )
        }.filter { it.ytelser.isNotEmpty() }
    }
}

data class SamhandlerForholdDto(
    val ordning: TpOrdning, val ytelser: List<SamhandlerYtelseDto>,
)

data class TpOrdning(
    val navn: String,
    val tpNr: String,
    val orgNr: String,
)

data class SamhandlerYtelseDto(
    val datoInnmeldtYtelseFom: LocalDate?, // Nullable to handle null values
    val ytelseType: YtelseTypeCode,
    val datoYtelseIverksattFom: LocalDate,
    val datoYtelseIverksattTom: LocalDate?, // Nullable to handle null values
    val ytelseId: Long
)
