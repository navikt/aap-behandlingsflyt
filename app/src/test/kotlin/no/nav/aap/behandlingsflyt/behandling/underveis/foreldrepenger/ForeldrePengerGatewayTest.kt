package no.nav.aap.behandlingsflyt.behandling.underveis.foreldrepenger

import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.verdityper.Periode
import org.junit.jupiter.api.Disabled
import java.time.LocalDate

class ForeldrePengerGatewayTest {

    @Disabled
    fun kanHenteDataFraForeldrePenger() {
        Fakes().use {
            val fpGateway = ForeldrepengerGateway()
            val request = ForeldrepengerRequest(
                Aktør("11111111111"),
                Periode(LocalDate.now().minusYears(1), LocalDate.now())
            )

            fpGateway.hentVedtakYtelseForPerson(request)
        }
    }
}