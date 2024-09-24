package no.nav.aap.behandlingsflyt.behandling.underveis.sykepenger

import no.nav.aap.behandlingsflyt.test.Fakes
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

@Fakes
class SykepengerGatewayTest {
    @Test
    fun kanHenteInformasjonFraSykepenger() {
        val spGateway = SykepengerGateway()
        val request = SykepengerRequest(
            setOf("12345678910"),
            LocalDate.now(),
            LocalDate.now().plusDays(1),
        )

        val response = spGateway.hentYtelseSykepenger(request)
        assertEquals(50, response.utbetaltePerioder[3].grad)
    }
}