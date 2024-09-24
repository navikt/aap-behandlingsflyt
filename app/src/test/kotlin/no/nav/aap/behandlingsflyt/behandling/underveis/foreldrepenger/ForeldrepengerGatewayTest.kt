package no.nav.aap.behandlingsflyt.behandling.underveis.foreldrepenger

import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

@Fakes
class ForeldrepengerGatewayTest {
    @Test
    fun kanHenteDataFraForeldrepenger() {
        val fpGateway = ForeldrepengerGateway()
        val request = ForeldrepengerRequest(
            Aktør("11111111111"),
            Periode(LocalDate.now().minusYears(1), LocalDate.now())
        )

        val response = fpGateway.hentVedtakYtelseForPerson(request)
        assertEquals(100.0, response.ytelser[0].anvist[0].utbetalingsgrad.verdi)
    }
}