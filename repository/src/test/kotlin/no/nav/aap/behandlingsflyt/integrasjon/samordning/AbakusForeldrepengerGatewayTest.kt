package no.nav.aap.behandlingsflyt.integrasjon.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Aktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class AbakusForeldrepengerGatewayTest {
    @Test
    fun kanHenteDataFraForeldrepenger() {
        val person = TestPerson(
            foreldrepenger = listOf(
                TestPerson.ForeldrePenger(
                    50,
                    Periode(LocalDate.now().minusYears(1), LocalDate.now())
                )
            )
        )
        FakePersoner.leggTil(person)
        val fpGateway = AbakusForeldrepengerGateway()
        val request = ForeldrepengerRequest(
            Aktør(person.identer.first().identifikator),
            Periode(LocalDate.now().minusYears(1), LocalDate.now())
        )

        val response = fpGateway.hentVedtakYtelseForPerson(request)
        assertEquals(50, response.ytelser[0].anvist[0].utbetalingsgrad.verdi)
    }
}