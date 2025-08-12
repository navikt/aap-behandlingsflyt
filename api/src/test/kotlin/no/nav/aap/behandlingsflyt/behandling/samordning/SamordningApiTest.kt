package no.nav.aap.behandlingsflyt.behandling.samordning

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.EnhetGateway
import no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning.SamordningYtelseDTO
import no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning.SamordningYtelseVurderingGrunnlagDTO
import no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning.samordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomInfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NorgGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningVurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningYtelseRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTjenestePensjonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate


@Fakes
@ExtendWith(BaseApiTest::class)
class SamordningApiKtTest : BaseApiTest() {
    private val repositoryRegistry = RepositoryRegistry()
        .register<InMemorySamordningVurderingRepository>()
        .register<InMemorySamordningYtelseRepository>()
        .register<InMemoryBehandlingRepository>()
        .register<InMemoryTjenestePensjonRepository>()

    @BeforeEach
    fun beforeEach() {
        GatewayRegistry.register<NomInfoGateway>()
        GatewayRegistry.register<NorgGateway>()
    }


    @Test
    fun `hente ut samordningsgrunnlag fra API`() {
        val ds = MockDataSource()
        val behandling = opprettBehandling(nySak(), TypeBehandling.Revurdering)

        InMemorySamordningYtelseRepository.lagre(
            behandling.id,
            listOf(
                SamordningYtelse(
                    ytelseType = Ytelse.PLEIEPENGER,
                    ytelsePerioder = listOf(
                        SamordningYtelsePeriode(
                            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1).minusDays(1)),
                            gradering = Prosent(50),
                            kronesum = null,
                        )
                    ),
                    kilde = "kilde",
                    saksRef = "saksref",
                ),
            ),
        )

        testApplication {
            installApplication {
                samordningGrunnlag(ds, repositoryRegistry, GatewayProvider)
            }

            val jwt = issueToken("nav:aap:afpoffentlig.read")
            val response = createClient().get("/api/behandling/${behandling.referanse.referanse}/grunnlag/samordning") {
                header("Authorization", "Bearer ${jwt.serialize()}")
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            assertThat(response.body<SamordningYtelseVurderingGrunnlagDTO>()).isEqualTo(
                SamordningYtelseVurderingGrunnlagDTO(
                    harTilgangTilÅSaksbehandle = true,
                    ytelser = listOf(
                        SamordningYtelseDTO(
                            ytelseType = Ytelse.PLEIEPENGER,
                            kilde = "kilde",
                            saksRef = "saksref",
                            periode =
                                Periode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().plusYears(1).minusDays(1),
                                ),
                            gradering = 50,
                            kronesum = null,
                            endringStatus = EndringStatus.NY
                        )
                    ),
                    vurderinger = listOf(),
                    begrunnelse = null,
                    fristNyRevurdering = null,
                    maksDatoEndelig = null,
                    tpYtelser = null,
                    vurdertAv = null
                )
            )
        }
    }
}
