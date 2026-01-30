package no.nav.aap.behandlingsflyt.integrasjon.dagpenger

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerKilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerYtelseType
import no.nav.aap.behandlingsflyt.integrasjon.samordning.DagpengerGatewayImpl
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class DagpengerGatewayImplTest {

    @Test
    fun `Kan hente data fra Dagpenger`() {
        val person = TestPerson(
            dagpenger = listOf(
                DagpengerPeriode(
                    Periode(
                        LocalDate.now(),
                        LocalDate.now().minusYears(1)
                    ),
                    kilde = DagpengerKilde.ARENA,
                    dagpengerYtelseType = DagpengerYtelseType.DAGPENGER_ARBEIDSSOKER_ORDINAER
                )
            )
        )



        FakePersoner.leggTil(person)
        val dpGateway = DagpengerGatewayImpl()

        val response: List<DagpengerPeriode> = dpGateway.hentYtelseDagpenger(
            personidentifikatorer = person.identer.first().identifikator,
            fom = LocalDate.now().minusYears(1),
            tom = LocalDate.now()
        )

        assertThat(response).isNotEmpty()
        assertEquals(1, response.size)

    }

}