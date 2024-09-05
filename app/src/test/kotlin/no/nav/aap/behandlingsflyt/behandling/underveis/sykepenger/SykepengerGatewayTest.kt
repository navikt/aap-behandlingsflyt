package no.nav.aap.behandlingsflyt.behandling.underveis.sykepenger

import no.nav.aap.behandlingsflyt.test.Fakes
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykepengerGatewayTest{

    @Test
    fun kanHenteSykepenger() {
        Fakes().use {
            val spGateway = SykepengerGateway()
            val request = SykePengerRequest(
                setOf("12345678910"),
                LocalDate.now(),
                LocalDate.now().plusDays(1),
            )

            spGateway.hentYtelseSykepenger(request)
        }
    }
}