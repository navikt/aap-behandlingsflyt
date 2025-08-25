package no.nav.aap.behandlingsflyt.integrasjon.medlemsskap

import java.time.LocalDate

data class MedlemskapRequest(
    val personident: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val inkluderSporingsinfo: Boolean
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