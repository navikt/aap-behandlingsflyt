package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.adapter

enum class SakTypeCode {
    UFORE
}

data class UføreRequest (
    val fnr: List<String>,
    val fom: String,
    val sakstype: SakTypeCode = SakTypeCode.UFORE
)
