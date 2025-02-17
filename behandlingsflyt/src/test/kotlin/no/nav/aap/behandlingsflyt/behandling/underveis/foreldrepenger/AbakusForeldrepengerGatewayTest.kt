package no.nav.aap.behandlingsflyt.behandling.underveis.foreldrepenger

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Aktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.AbakusForeldrepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class AbakusForeldrepengerGatewayTest {
    @Test
    fun kanHenteDataFraForeldrepenger() {
        val fpGateway = AbakusForeldrepengerGateway()
        val request = ForeldrepengerRequest(
            Aktør("11111111111"),
            Periode(LocalDate.now().minusYears(1), LocalDate.now())
        )

        val response = fpGateway.hentVedtakYtelseForPerson(request)
        assertEquals(100.0, response.ytelser[0].anvist[0].utbetalingsgrad.verdi)
    }
}