package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

data class Medlemskap(val unntak: List<Unntak>)

data class Unntak(
    val status: String,
    val statusaarsak: String?,
    val medlem: Boolean,
    val grunnlag: String,
    val lovvalg: String,
    val helsedel: Boolean,
)