package no.nav.aap.behandlingsflyt.behandling.underveis.sykepenger

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerRequest
import no.nav.aap.behandlingsflyt.test.Fakes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class SykepengerGatewayTest {
    @Test
    fun kanHenteInformasjonFraSykepenger() {
        val spGateway = SykepengerGateway()
        val request = SykepengerRequest(
            setOf("11111111111"),
            LocalDate.now(),
            LocalDate.now().plusDays(1),
        )

        val response = spGateway.hentYtelseSykepenger(request)
        assertEquals(50, response.utbetaltePerioder[3].grad)
    }
}