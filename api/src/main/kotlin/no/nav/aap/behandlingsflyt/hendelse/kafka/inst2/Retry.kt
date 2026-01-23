package no.nav.aap.behandlingsflyt.hendelse.kafka.inst2

sealed class RetryResult<T> {
    data class Success<T>(
        val content: T,
        val previousExceptions: List<Exception> = emptyList(),
    ) : RetryResult<T>()

    data class Failure<T>(
        val exceptions: List<Exception> = emptyList(),
    ) : RetryResult<T>() {
        fun samlaExceptions(): Exception = samleExceptions(this.exceptions)
    }
}

suspend fun <T> retry(
    times: Int = 2,
    vent: (timesLeft: Int) -> Unit = {},
    block: suspend () -> T,
) = retryInner(times, vent, emptyList(), block)

private suspend fun <T> retryInner(
    times: Int,
    vent: (timesLeft: Int) -> Unit,
    exceptions: List<Exception>,
    block: suspend () -> T,
): RetryResult<T> =
    try {
        RetryResult.Success(block(), exceptions)
    } catch (ex: Exception) {
        if (times < 1) {
            RetryResult.Failure(exceptions + ex)
        } else {
            val timesLeft = times - 1
            vent(timesLeft)
            retryInner(times - 1, vent, exceptions + ex, block)
        }
    }

private fun samleExceptions(exceptions: List<Exception>): Exception {
    val siste = exceptions.last()
    exceptions.dropLast(1).reversed().forEach { siste.addSuppressed(it) }
    return siste
}

