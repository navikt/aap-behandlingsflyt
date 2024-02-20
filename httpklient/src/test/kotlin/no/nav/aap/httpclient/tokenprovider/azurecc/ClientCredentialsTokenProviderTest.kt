package no.nav.aap.httpclient.tokenprovider.azurecc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ClientCredentialsTokenProviderTest {
    @Test
    fun `skal generer tidspunkt frem i tid på utløpstidspunkt`() {

        val expiresTime = calculateExpiresTime(3600)
        val inTheFuture = LocalDateTime.now().plusSeconds(3550)
        val evenMoreinTheFuture = LocalDateTime.now().plusSeconds(3600)

        assertThat(inTheFuture).isBefore(expiresTime)
        assertThat(evenMoreinTheFuture).isAfter(expiresTime)
    }
}