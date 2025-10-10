package no.nav.aap.behandlingsflyt.integrasjon.samordning

import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class AbakusSykepengerGatewayTest {
    @Test
    fun `kan hente informasjon fra sykepenger`() {
        val person = TestPerson(
            sykepenger = listOf(
                TestPerson.Sykepenger(
                    grad = 50,
                    Periode(LocalDate.now(), LocalDate.now().plusDays(1))
                )
            )
        )
        FakePersoner.leggTil(person)

        val spGateway = AbakusSykepengerGateway()

        val response = spGateway.hentYtelseSykepenger(
            person.identer.map { it.identifikator }.toSet(),
            LocalDate.now(),
            LocalDate.now().plusDays(1),
        )
        assertThat(response).hasSize(1)
        assertThat(response[0].grad).isEqualTo(50)
    }
}