package no.nav.aap.behandlingsflyt.integrasjon.dagpenger

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerPeriodeResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerResponse
import no.nav.aap.behandlingsflyt.integrasjon.samordning.DagpengerGatewayImpl
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class DagpengerGatewayImplTest {
    @Test
    fun `Kan hente data fra Dagpenger`() {
        val person = TestPerson(
            dagpenger = DagpengerResponse(
               personIdent = "12345",
                perioder = listOf(
                    DagpengerPeriodeResponse(
                        fraOgMedDato = LocalDate.now(),
                        tilOgMedDato = LocalDate.now().minusYears(1),
                        kilde = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerKilde.ARENA,
                        ytelseType = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerYtelseType.DAGPENGER_ARBEIDSSOKER_ORDINAER
                    )
                )
            )
        )

        FakePersoner.leggTil(person)
        val dpGateway = DagpengerGatewayImpl()

        val response: List<DagpengerPeriodeResponse> = dpGateway.hentYtelseDagpenger(
            personidentifikatorer = person.identer.first().identifikator,
            fom = LocalDate.now().minusYears(1).toString(),
            tom = LocalDate.now().toString()
        )

        assertThat(response).isNotEmpty()
        assertEquals(1, response.size)

    }

}