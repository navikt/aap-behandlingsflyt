package no.nav.aap.behandlingsflyt.behandling.trekkklage.flate


data class TrekkKlageVurderingDto (
    val begrunnelse: String,
    val skalTrekkes: Boolean,
    val hvorforTrekkes: TrekkKlageÅrsakDto?,
)

enum class TrekkKlageÅrsakDto {
    TRUKKET_AV_BRUKER,
    FEILREGISTRERING;
}