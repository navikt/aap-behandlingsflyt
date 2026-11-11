package no.nav.aap.behandlingsflyt.behandling.brev

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.Dokumentkilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomInfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NorgGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.test.AzureTokenGen
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Fakes
class SykdomsvurderingForBrevApiTest : BaseApiTest() {

    @Test
    fun `henter grunnlag uten vurdering`() {
        val behandling = opprettBehandling(opprettInMemorySak(), TypeBehandling.Førstegangsbehandling)

        val response = hentGrunnlag(behandling.referanse.referanse)

        assertThat(response.vurdering).isNull()
        assertThat(response.historiskeVurderinger).isEmpty()
    }

    @Test
    fun `henter dokumentkilder og vurderingsmetadata for gjeldende og historiske vurderinger`() {
        val sak = opprettInMemorySak()
        val førsteBehandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)
        val andreBehandling = opprettBehandling(sak, TypeBehandling.Revurdering, førsteBehandling.id)
        val behandling = opprettBehandling(sak, TypeBehandling.Revurdering, andreBehandling.id)
        val eldste = SykdomsvurderingForBrev(
            behandlingId = førsteBehandling.id,
            vurdering = "Første vurdering",
            vurdertAv = Bruker("Z123456"),
            vurdertTidspunkt = LocalDateTime.of(2024, 2, 1, 10, 0),
            kilder = listOf(kilde("100", "101", "Første legeerklæring", LocalDate.of(2024, 1, 15))),
        )
        val historisk = SykdomsvurderingForBrev(
            behandlingId = andreBehandling.id,
            vurdering = "Historisk vurdering",
            vurdertAv = Bruker("Z654321"),
            vurdertTidspunkt = LocalDateTime.of(2024, 5, 1, 11, 0),
            kilder = listOf(kilde("200", "201", "Spesialisterklæring", LocalDate.of(2024, 4, 15))),
        )
        val gjeldende = SykdomsvurderingForBrev(
            behandlingId = behandling.id,
            vurdering = "Gjeldende vurdering",
            vurdertAv = Bruker("Z123456"),
            vurdertTidspunkt = LocalDateTime.of(2024, 8, 1, 12, 0),
            kilder = listOf(
                kilde("300", "301", "Oppdatert legeerklæring", LocalDate.of(2024, 7, 15)),
                kilde("300", "302", "Vedlegg", LocalDate.of(2024, 7, 15)),
            ),
        )
        listOf(eldste, historisk, gjeldende).forEach {
            InMemorySykdomsvurderingForBrevRepository.lagre(it.behandlingId, it)
        }

        val response = hentGrunnlag(behandling.referanse.referanse)

        assertVurdering(response.vurdering, gjeldende)
        val historiskeVurderinger = response.historiskeVurderinger
        assertThat(historiskeVurderinger).hasSize(2)
        assertVurdering(historiskeVurderinger[0], historisk)
        assertVurdering(historiskeVurderinger[1], eldste)
    }

    @Test
    fun `gamle vurderinger uten dokumentkilder returneres med tom kildeliste`() {
        val sak = opprettInMemorySak()
        val førsteBehandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)
        val behandling = opprettBehandling(sak, TypeBehandling.Revurdering, førsteBehandling.id)
        val historisk = SykdomsvurderingForBrev(
            behandlingId = førsteBehandling.id,
            vurdering = "Gammel vurdering uten kilder",
            vurdertAv = Bruker("Z654321"),
            vurdertTidspunkt = LocalDateTime.of(2024, 2, 1, 10, 0),
        )
        val gjeldende = SykdomsvurderingForBrev(
            behandlingId = behandling.id,
            vurdering = "Gjeldende vurdering uten kilder",
            vurdertAv = Bruker("Z123456"),
            vurdertTidspunkt = LocalDateTime.of(2024, 5, 1, 11, 0),
        )
        listOf(historisk, gjeldende).forEach {
            InMemorySykdomsvurderingForBrevRepository.lagre(it.behandlingId, it)
        }

        val response = hentGrunnlag(behandling.referanse.referanse)

        assertVurdering(response.vurdering, gjeldende)
        assertThat(response.historiskeVurderinger).hasSize(1)
        assertVurdering(response.historiskeVurderinger[0], historisk)
    }

    private fun kilde(
        journalpostId: String,
        dokumentInfoId: String,
        dokumentnavn: String,
        journalføringsdato: LocalDate,
    ) = Dokumentkilde(journalpostId, dokumentInfoId, dokumentnavn, journalføringsdato)

    private fun assertVurdering(response: SykdomsvurderingForBrevVurderingDto?, forventet: SykdomsvurderingForBrev) {
        val vurdering = requireNotNull(response)
        assertThat(vurdering.vurdering).isEqualTo(forventet.vurdering)
        assertThat(vurdering.vurderingerMeta.vurdertAv?.ident).isEqualTo(forventet.vurdertAv.ident)
        assertThat(vurdering.vurderingerMeta.vurdertAv?.dato).isEqualTo(forventet.vurdertTidspunkt.toLocalDate())
        assertThat(vurdering.kilder).containsExactlyElementsOf(forventet.kilder)
    }

    private fun hentGrunnlag(referanse: UUID): SykdomsvurderingForBrevDto {
        lateinit var respons: SykdomsvurderingForBrevDto
        testApplication {
            installApplication {
                sykdomsvurderingForBrevApi(
                    dataSource = MockDataSource(),
                    repositoryRegistry = inMemoryRepositoryRegistry,
                    gatewayProvider = createGatewayProvider {
                        register<NomInfoGateway>()
                        register<NorgGateway>()
                    },
                )
            }

            val response = createClient().get("/api/behandling/$referanse/grunnlag/sykdomsvurdering-for-brev") {
                bearerAuth(AzureTokenGen("behandlingsflyt").generate(false, "behandlingsflyt", "Z123456"))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            respons = response.body()
        }
        return respons
    }
}
