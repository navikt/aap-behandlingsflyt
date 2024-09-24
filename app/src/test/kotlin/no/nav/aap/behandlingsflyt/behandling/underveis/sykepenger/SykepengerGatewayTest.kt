package no.nav.aap.behandlingsflyt.behandling.underveis.sykepenger

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerRequest
import no.nav.aap.behandlingsflyt.test.Fakes
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

@Disabled
class SykepengerGatewayTest{
    companion object {
        private val fakes = Fakes()

        @JvmStatic
        @AfterAll
        fun afterAll() {
            fakes.close()
        }
    }

    @Disabled
    @Test
    fun kanHenteInformasjonFraSykepenger() {
        val spGateway = SykepengerGateway()
        val request = SykepengerRequest(
            setOf("12345678910"),
            LocalDate.now(),
            LocalDate.now().plusDays(1),
        )

        val response = spGateway.hentYtelseSykepenger(request)
        assertEquals( 50, response.utbetaltePerioder.get(3).grad)
    }
}