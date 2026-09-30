package no.nav.aap.behandlingsflyt.behandling.klage.klagebehandling

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagevurderingNay
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetVedtakType
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.AzureTokenGen
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.ImMemoryKlagebehandlingNayRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPåklagetBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@Fakes
class KlagebehandlingNayGrunnlagApiTest : BaseApiTest() {

    @Test
    fun `skal returnere Kelvin behandling som påklaget vedtakstype med vurdering`() {
        val sak = opprettInMemorySak()
        val påklagetBehandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)
        val klagebehandling = opprettBehandling(sak, TypeBehandling.Klage)
        lagrePåklagetBehandling(
            klagebehandling.id,
            PåklagetBehandlingVurdering(
                påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
                påklagetBehandling = påklagetBehandling.id,
                påklagetTilbakekrevingsbehandling = null,
                vurdertAv = Bruker("Z123456"),
                opprettet = Instant.now(),
            ),
        )
        ImMemoryKlagebehandlingNayRepository.lagre(klagebehandling.id, klagevurdering())

        val respons = hentGrunnlag(klagebehandling.referanse.referanse.toString())

        assertThat(respons.at("/påklagetVedtakType").asText())
            .isEqualTo(PåklagetVedtakType.KELVIN_BEHANDLING.name)
        assertThat(respons.path("vurdering").isObject).isTrue()
    }

    @Test
    fun `skal returnere tilbakekreving som påklaget vedtakstype uten vurdering`() {
        val klagebehandling = opprettBehandling(opprettInMemorySak(), TypeBehandling.Klage)
        lagrePåklagetBehandling(
            klagebehandling.id,
            PåklagetBehandlingVurdering(
                påklagetVedtakType = PåklagetVedtakType.TILBAKEKREVING,
                påklagetBehandling = null,
                påklagetTilbakekrevingsbehandling = UUID.randomUUID(),
                vurdertAv = Bruker("Z123456"),
                opprettet = Instant.now(),
            ),
        )

        val respons = hentGrunnlag(klagebehandling.referanse.referanse.toString())

        assertThat(respons.at("/påklagetVedtakType").asText())
            .isEqualTo(PåklagetVedtakType.TILBAKEKREVING.name)
        assertThat(respons.path("vurdering").isNull).isTrue()
    }

    private fun lagrePåklagetBehandling(
        behandlingId: BehandlingId,
        vurdering: PåklagetBehandlingVurdering,
    ) {
        InMemoryPåklagetBehandlingRepository.lagre(behandlingId, vurdering)
    }

    private fun klagevurdering() = KlagevurderingNay(
        begrunnelse = "begrunnelse",
        notat = null,
        innstilling = KlageInnstilling.OPPRETTHOLD,
        vilkårSomOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5),
        vilkårSomOmgjøres = emptyList(),
        vurdertAv = Bruker("Z123456"),
        opprettet = Instant.now(),
    )

    private fun hentGrunnlag(referanse: String): JsonNode {
        lateinit var respons: JsonNode

        testApplication {
            installApplication {
                klagebehandlingNayGrunnlagApi(
                    dataSource = MockDataSource(),
                    repositoryRegistry = inMemoryRepositoryRegistry,
                    gatewayProvider = testGatewayProvider(),
                )
            }

            val response = createClient().get("/api/klage/$referanse/grunnlag/klagebehandling-nay") {
                bearerAuth(AzureTokenGen("behandlingsflyt").generate(false, "behandlingsflyt", "Z123456"))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            respons = response.body()
        }

        return respons
    }
}
