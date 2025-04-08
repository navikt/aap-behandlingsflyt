package no.nav.aap.behandlingsflyt.exception

interface ErrorRespons {
    val message: String?
}

data class GenerellErrorRespons(
    override val message: String?
) : ErrorRespons

data class ApiErrorRespons(
    override val message: String,
    val code: String? = null,
) : ErrorRespons
