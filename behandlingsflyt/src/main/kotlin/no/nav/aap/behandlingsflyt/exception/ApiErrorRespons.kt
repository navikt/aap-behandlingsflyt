package no.nav.aap.behandlingsflyt.exception

data class ApiErrorResponse(
    val message: String,
    val code: String? = null,
)
