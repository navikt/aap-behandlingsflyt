package no.nav.aap.behandlingsflyt.kontrakt.behandling

public data class MarkeringDto(
    val markeringType: MarkeringType,
    val begrunnelse: String,
    val opprettetAv: String? = null,
    )

public enum class MarkeringType {
    HASTER,
    KREVER_SPESIALKOMPETANSE
}