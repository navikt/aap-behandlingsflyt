package no.nav.aap.behandlingsflyt.behandling.klage.påklagetbehandling

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingBehandlingsstatus
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingshendelse
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.test.AzureTokenGen
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTilbakekrevingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Fakes
class PåklagetBehandlingGrunnlagApiTest : BaseApiTest() {

    @Test
    fun `skal returnere avsluttet tilbakekreving når unleash-toggle er fjernet`() {
        val sak = opprettInMemorySak()
        val klagebehandling = opprettBehandling(sak, TypeBehandling.Klage)
        val tilbakekrevingReferanse = UUID.randomUUID()
        InMemoryTilbakekrevingRepository.lagre(
            sak.id,
            Tilbakekrevingshendelse(
                tilbakekrevingBehandlingId = tilbakekrevingReferanse,
                eksternFagsakId = sak.saksnummer.toString(),
                hendelseOpprettet = LocalDateTime.now(),
                eksternBehandlingId = null,
                sakOpprettet = LocalDateTime.now(),
                varselSendt = null,
                venteGrunn = null,
                gjenopptas = null,
                behandlingsstatus = TilbakekrevingBehandlingsstatus.AVSLUTTET,
                totaltFeilutbetaltBeløp = Beløp(1000),
                tilbakekrevingSaksbehandlingUrl = URI.create("https://nav.no/behandling/$tilbakekrevingReferanse"),
                fullstendigPeriode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                versjon = 1,
                vedtaksdato = LocalDate.now(),
            ),
        )

        val respons = hentGrunnlag(klagebehandling.referanse.referanse.toString())

        assertThat(respons.at("/avsluttaTilbakekrevingsbehandlinger/0/referanse").asText())
            .isEqualTo(tilbakekrevingReferanse.toString())
    }

    private fun hentGrunnlag(referanse: String): JsonNode {
        lateinit var respons: JsonNode

        testApplication {
            installApplication {
                påklagetBehandlingGrunnlagApi(
                    dataSource = MockDataSource(),
                    repositoryRegistry = inMemoryRepositoryRegistry,
                    gatewayProvider = testGatewayProvider(),
                )
            }

            val response = createClient().get("/api/klage/$referanse/grunnlag/påklaget-behandling") {
                bearerAuth(AzureTokenGen("behandlingsflyt").generate(false, "behandlingsflyt", "Z123456"))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            respons = response.body()
        }

        return respons
    }
}
