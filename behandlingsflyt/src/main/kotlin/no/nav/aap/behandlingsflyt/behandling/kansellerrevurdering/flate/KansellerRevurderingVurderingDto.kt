package no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.flate

data class KansellerRevurderingVurderingDto(
    val årsak: KansellerRevurderingÅrsakDto?,
    val begrunnelse: String
)

enum class KansellerRevurderingÅrsakDto {
    REVURDERING_ER_IKKE_LENGER_AKTUELL,
    REVURDERINGEN_ER_FEILREGISTRERT,
    REVURDERINGEN_ER_AVBRUTT_PÅ_GRUNN_AV_FEIL,
    ANNET
}
