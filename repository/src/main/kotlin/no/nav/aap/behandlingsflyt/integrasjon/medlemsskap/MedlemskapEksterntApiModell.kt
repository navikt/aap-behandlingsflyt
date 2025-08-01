package no.nav.aap.behandlingsflyt.integrasjon.medlemsskap

import no.nav.aap.komponenter.type.Periode

data class MedlemskapRequest(
    val ident: String,
    val periode: Periode
)

// Swagger-doc her: https://medlemskap-medl-api.dev.intern.nav.no/swagger-ui/index.html
data class MedlemskapResponse(
    val unntakId: Number,
    val ident: String,
    val fraOgMed: String,
    val tilOgMed: String,
    val status: String,
    val statusaarsak: String?,
    val medlem: Boolean,
    val grunnlag: String,
    val lovvalg: String,
    val helsedel: Boolean,
    val lovvalgsland: String?,
    val sporingsinformasjon: Sporingsinformasjon?
)

data class Sporingsinformasjon(
    val kilde: String?
)