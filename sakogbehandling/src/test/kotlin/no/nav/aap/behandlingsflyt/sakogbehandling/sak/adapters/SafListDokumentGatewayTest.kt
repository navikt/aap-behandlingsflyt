package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.SafHentDokumentGateway.Companion.extractFileNameFromHeaders
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.net.http.HttpHeaders

class SafListDokumentGatewayTest {
    companion object {
        val fakes = Fakes()

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            fakes.close()
        }
    }
    @Test
    fun `hente dokumentoversikt fra saf`() {
        val gateway = SafListDokumentGateway

        // TODO: Generer fake token
        val token = OidcToken(
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6InE3UDFOdnh1R1F3RE4yVGFpTW92alo4YVp3cyJ9.eyJhdWQiOiI0MjBhNmIzNy04NmU0LTQxMzYtOWRjOC03YTI0NDNjZDg1MjUiLCJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vOTY2YWM1NzItZjViNy00YmJlLWFhODgtYzc2NDE5YzBmODUxL3YyLjAiLCJpYXQiOjE3MTg5NjI2NTUsIm5iZiI6MTcxODk2MjY1NSwiZXhwIjoxNzE4OTY4MTMxLCJhaW8iOiJBVVFBdS84WEFBQUFjN1JXVFpmdEdtZVlpN3JwcGpQTFlkcGpnY1g1djY1SU1mbWhkOWkzMVhVbnZOeExNejlQL2hIdU9XdnRTTjBIVGtZWUdpZkZ1OThxeXRodWlEemlDZz09IiwiYXpwIjoiNDkyMGZmMWQtMGY2Yi00MDdmLTg2MzAtNjk1ZTMzNjBkM2QzIiwiYXpwYWNyIjoiMiIsImdyb3VwcyI6WyI4YmIwZWUxMy00OWNkLTRlNzUtOGMzZC1hMTM0MjBjOGIzNzYiLCJkZWMzZWU1MC1iNjgzLTQ2NDQtOTUwNy01MjBlOGYwNTRhYzIiLCIxMjM1MzY3OS1hYTgwLTRlNTktYmI0Ny05NWU3MjdiZmU4NWMiXSwibmFtZSI6IkZfWjk5NDU3MyBFX1o5OTQ1NzMiLCJvaWQiOiJkMDA4YmUzNy1kZjE0LTQ2NjItOGNhYi01NTE0N2YxMDRhMzUiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJGX1o5OTQ1NzMuRV9aOTk0NTczQHRyeWdkZWV0YXRlbi5ubyIsInJoIjoiMC5BVWNBY3NWcWxyZjF2a3VxaU1ka0djRDRVVGRyQ2tMa2hqWkJuY2g2SkVQTmhTVU5BUlUuIiwic2NwIjoiZGVmYXVsdGFjY2VzcyIsInN1YiI6IkpkbnJ0SkE4MGQ3U1VETWtRYlRGZFJqbUhURVRQY1FMZVdGdVloSFo2blkiLCJ0aWQiOiI5NjZhYzU3Mi1mNWI3LTRiYmUtYWE4OC1jNzY0MTljMGY4NTEiLCJ1dGkiOiJRbUxiTkVIQmdVS2RzNDdzWjJjT0FBIiwidmVyIjoiMi4wIiwiTkFWaWRlbnQiOiJaOTk0NTczIiwiYXpwX25hbWUiOiJkZXYtZ2NwOmFhcDpzYWtzYmVoYW5kbGluZyJ9.u9wkZ0PCQdRcnl-RgW65LOohYc2Hz3VUxy0YKEaGFSp7LkIiifWi_l7Q2iYMkX7RKAnPyRPKIvCXZln1IvHxGzwmmmmpSMN8Xm0fN41MbKanwHMFBizIpVudxZ-vSmPGuBb6FnQS4dZs0eEFi5esoosE4LkygnDv9ueVquyvTjZ5nFcRYLBvv4KMPkn6FJZ3kGMZFy-t9-OwDu4UMUCFkeXs7bR4JY0BnC1v2M5QYHg9eL5y7yDVxoX48kU_vMLnG-0oLuy9TMqKlPojKspOgKE1sRs51AEYVl7xgZ_rCFZN8gDSulhawUak5AiDKVWR6JpnPfj_MLGQwZbYMrFIxw"
        )

        val documents = gateway.hentDokumenterForSak(Saksnummer("123"), token)
        assertThat(documents).hasSize(3)
    }

    @Test
    fun `hente ut filnavn fra header`() {
        val headers =
            HttpHeaders.of(mapOf("Content-Disposition" to listOf("inline; filename=400000000_ARKIV.pdf"))) { _, _ -> true }

        val response = extractFileNameFromHeaders(headers)

        assertThat(response).isEqualTo("400000000_ARKIV.pdf")
    }
}