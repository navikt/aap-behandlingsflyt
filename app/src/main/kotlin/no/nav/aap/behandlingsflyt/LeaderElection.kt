package no.nav.aap.behandlingsflyt

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.aap.komponenter.config.requiredConfigForKey
import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun isLeader(log: io.ktor.util.logging.Logger): Boolean {
    val electorUrl = requiredConfigForKey("ELECTOR_GET_URL")
    val client = HttpClient.newHttpClient()
    val response = client.send(
        HttpRequest.newBuilder().uri(URI.create(electorUrl)).GET().build(),
        HttpResponse.BodyHandlers.ofString()
    )
    val json = ObjectMapper().readTree(response.body())
    val leaderHostname = json.get("name").asText()
    val hostname = InetAddress.getLocalHost().hostName
    log.info("electorUrl=${electorUrl}, leaderHostname=$leaderHostname, hostname=$hostname")
    return hostname == leaderHostname
}
