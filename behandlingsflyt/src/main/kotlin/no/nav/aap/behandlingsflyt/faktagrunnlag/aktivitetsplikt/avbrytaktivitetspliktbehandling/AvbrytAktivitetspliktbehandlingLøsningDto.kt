package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling

import java.time.LocalDateTime

data class AvbrytAktivitetspliktbehandlingLøsningDto(
    val årsak: AvbrytAktivitetspliktbehandlingÅrsakDto,
    val begrunnelse: String,
    val opprettetTidspunkt: LocalDateTime? = null,
)

enum class AvbrytAktivitetspliktbehandlingÅrsakDto {
    BEHANDLINGEN_BLE_OPPRETTET_VED_EN_FEIL,
    DET_HAR_OPPSTAATT_EN_FEIL_OG_BEHANDLINGEN_MAA_STARTES_PAA_NYTT
}