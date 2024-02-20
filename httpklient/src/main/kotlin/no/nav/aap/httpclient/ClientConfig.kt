package no.nav.aap.httpclient

import java.time.Duration
import java.util.function.Supplier


class ClientConfig(
    val scope: String? = null,
    val connectionTimeout: Duration = Duration.ofSeconds(15),
    val parseableHttpStatuses: List<Int> = emptyList(),
    val additionalHeaders: List<Pair<String, String>> = emptyList(),
    val additionalFunctionalHeaders: List<Pair<String, Supplier<String>>> = emptyList()
) {
    fun isParseableStatus(status: Int): Boolean {
        return parseableHttpStatuses.contains(status)
    }
}