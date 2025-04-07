package no.nav.aap.behandlingsflyt.exception

import io.ktor.http.*

enum class ApiErrorCode {
    INTERNFEIL,
    UKJENT_FEIL,
    IKKE_FUNNET,
    ENDEPUNKT_IKKE_FUNNET,
    UGYLDIG_FORESPØRSEL,
}

open class ApiException(
    open val status: HttpStatusCode,
    open val code: ApiErrorCode? = null,
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {
    fun tilApiErrorResponse() = ApiErrorResponse(
        message = message,
        code = code?.name ?: ApiErrorCode.UKJENT_FEIL.name
    )
}

class IkkeTillattException(
    override val message: String
) : ApiException(
    status = HttpStatusCode.Forbidden,
    message = message
)

class UgyldigForespørselException(
    override val status: HttpStatusCode = HttpStatusCode.BadRequest,
    override val message: String,
    override val cause: Throwable? = null,
) : ApiException(
    status = status,
    code = ApiErrorCode.UGYLDIG_FORESPØRSEL,
    message = message,
    cause = cause
)

class InternfeilException(
    override val message: String,
    override val cause: Throwable? = null,
) : ApiException(
    status = HttpStatusCode.InternalServerError,
    code = ApiErrorCode.INTERNFEIL,
    message = message,
    cause = cause
)
