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
    fun tilApiErrorResponse() = ApiErrorRespons(
        message = message,
        code = code?.name ?: ApiErrorCode.UKJENT_FEIL.name
    )
}

class VerdiIkkeFunnetException(
    override val message: String,
    override val code: ApiErrorCode? = null,
) : ApiException(
    status = HttpStatusCode.NotFound,
    message = message,
    code = code ?: ApiErrorCode.IKKE_FUNNET
)

class IkkeTillattException(
    override val message: String
) : ApiException(
    status = HttpStatusCode.Forbidden,
    message = message
)

class UgyldigForespørselException(
    override val message: String,
    override val code: ApiErrorCode = ApiErrorCode.UGYLDIG_FORESPØRSEL,
    override val cause: Throwable? = null,
) : ApiException(
    status = HttpStatusCode.BadRequest,
    code = code,
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
