package no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.flate

data class AvbrytRevurderingVurderingDto(
    val årsak: AvbrytRevurderingÅrsakDto?,
    val begrunnelse: String
)

enum class AvbrytRevurderingÅrsakDto {
    REVURDERINGEN_BLE_OPPRETTET_VED_EN_FEIL,
    DET_HAR_OPPSTAATT_EN_FEIL_OG_BEHANDLINGEN_MAA_STARTES_PAA_NYTT
}
