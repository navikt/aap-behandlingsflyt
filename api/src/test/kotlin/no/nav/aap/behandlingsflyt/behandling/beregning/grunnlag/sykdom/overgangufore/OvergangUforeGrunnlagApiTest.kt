package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangufore

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreSøknad
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomInfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NorgGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.LokalUnleash
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryOvergangUføreRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUføreSøknadRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

@Fakes
class OvergangUforeGrunnlagApiTest : BaseApiTest() {

    @Test
    fun `skal hente ut overgang uføre-vurderinger med grunnlagsdata`() {
        val ds = MockDataSource()
        val behandling = opprettBehandling(nySak(), TypeBehandling.Revurdering)
        val behandlingUtenUføreSøknad = opprettBehandling(nySak(), TypeBehandling.Revurdering)

        val vurdering = OvergangUføreVurdering(
            begrunnelse = "begrunnelse",
            brukerHarSøktOmUføretrygd = true,
            brukerHarFåttVedtakOmUføretrygd = null,
            brukerRettPåAAP = true,
            fom = LocalDate.now(),
            tom = null,
            vurdertAv = "abc123",
            vurdertIBehandling = behandling.id,
            opprettet = Instant.now()
        )
        val vurderingUtenSøknad = vurdering.copy(vurdertIBehandling = behandlingUtenUføreSøknad.id)
        InMemoryOvergangUføreRepository.lagre(
            behandlingId = behandling.id,
            overgangUføreVurderinger = listOf(vurdering)
        )
        InMemoryOvergangUføreRepository.lagre(
            behandlingId = behandlingUtenUføreSøknad.id,
            overgangUføreVurderinger = listOf(vurderingUtenSøknad)
        )
        val uføreSøknad = UføreSøknad(LocalDate.now(), 1L)
        InMemoryUføreSøknadRepository.lagre(
            behandlingId = behandling.id,
            uføreSøknad = uføreSøknad
        )

        val vurderingResponse = OvergangUføreVurderingResponse(
            begrunnelse = vurdering.begrunnelse,
            brukerHarSøktUføretrygd = vurdering.brukerHarSøktOmUføretrygd,
            brukerHarFåttVedtakOmUføretrygd = vurdering.brukerHarFåttVedtakOmUføretrygd,
            brukerRettPåAAP = vurdering.brukerRettPåAAP,
            fom = vurdering.fom,
            tom = vurdering.tom,
            vurderingerMeta = VurderingerMetaResponse(
                vurdertAv = VurdertAvResponse(vurdering.vurdertAv, LocalDate.now(), "Test Testesen", "Lokalenhetsnavn"),
                kvalitetssikretAv = null,
                besluttetAv = null,
            ),
        )
        val uføreSøknadOpplysninger = UføreSøknadOpplysninger(
            uføreSøknad.soknadsdato
        )

        testApplication {

            installApplication {
                overgangUforeGrunnlagApi(ds, inMemoryRepositoryRegistry, createGatewayProvider {
                    register<NomInfoGateway>()
                    register<NorgGateway>()
                    register<LokalUnleash>()
                })
            }

            val responseMedSøknad =
                createClient().get("/api/behandling/${behandling.referanse.referanse}/grunnlag/overgangufore") {
                    header("Authorization", "Bearer ${getToken().token()}")
                }
            val responseUtenSøknad =
                createClient().get("/api/behandling/${behandlingUtenUføreSøknad.referanse.referanse}/grunnlag/overgangufore") {
                    header("Authorization", "Bearer ${getToken().token()}")
                }
            assertThat(responseMedSøknad.status).isEqualTo(HttpStatusCode.OK)
            assertThat(responseUtenSøknad.status).isEqualTo(HttpStatusCode.OK)

            val overgangUføreGrunnlagResponse = responseMedSøknad.body<OvergangUføreGrunnlagResponse>()
            val overgangUføreGrunnlagUtenSøknadResponse = responseUtenSøknad.body<OvergangUføreGrunnlagResponse>()
            assertThat(overgangUføreGrunnlagResponse.nyeVurderinger).isEqualTo(listOf(vurderingResponse))
            assertThat(overgangUføreGrunnlagResponse.uføreSøknadOpplysninger).isEqualTo(uføreSøknadOpplysninger)

            assertThat(overgangUføreGrunnlagUtenSøknadResponse.uføreSøknadOpplysninger).isNull()
        }
    }
}