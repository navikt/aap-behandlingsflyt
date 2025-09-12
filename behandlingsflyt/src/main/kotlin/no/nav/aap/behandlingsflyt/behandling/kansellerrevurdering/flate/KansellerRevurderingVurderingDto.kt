package no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.flate

data class KansellerRevurderingVurderingDto(
    val årsak: KansellerRevurderingÅrsakDto?,
    val begrunnelse: String
)

enum class KansellerRevurderingÅrsakDto {
    FEILREGISTRERING,
    START_REVURDERING_PAA_NYTT
}
