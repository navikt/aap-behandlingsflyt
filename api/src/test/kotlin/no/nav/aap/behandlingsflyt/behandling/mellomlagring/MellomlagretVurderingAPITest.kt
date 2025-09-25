package no.nav.aap.behandlingsflyt.behandling.mellomlagring

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMellomlagretVurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@Fakes
class MellomlagretVurderingAPITest : BaseApiTest() {
    @Test
    fun `hente ut mellomlagret vurdering fra API`() {
        val ds = MockDataSource()
        val behandling = opprettBehandling(nySak(), TypeBehandling.Revurdering)
        val avklaringsbehovKode = AvklaringsbehovKode.`5056`

        val mellomlagretVurdering = MellomlagretVurdering(
            behandlingId = behandling.id,
            avklaringsbehovKode = avklaringsbehovKode,
            data = """
                    {"element": "verdi", "tallElement": 1234, "boolskElement": true}
                    """.trimIndent(),
            vurdertAv = "A123456",
            vurdertDato = LocalDateTime.now().withNano(0)
        )
        InMemoryMellomlagretVurderingRepository.lagre(mellomlagretVurdering)

        testApplication {
            installApplication {
                mellomlagretVurderingApi(ds, inMemoryRepositoryRegistry)
            }

            val response =
                createClient().get("/api/behandling/mellomlagret-vurdering/${behandling.referanse.referanse}/${avklaringsbehovKode}") {
                    header("Authorization", "Bearer ${getToken().token()}")
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            assertThat(response.body<MellomlagretVurderingResponse>()).isEqualTo(
                MellomlagretVurderingResponse(
                    mellomlagretVurdering = MellomlagretVurderingDto(
                        behandlingId = mellomlagretVurdering.behandlingId,
                        avklaringsbehovkode = mellomlagretVurdering.avklaringsbehovKode,
                        data = mellomlagretVurdering.data,
                        vurdertAv = mellomlagretVurdering.vurdertAv,
                        vurdertDato = mellomlagretVurdering.vurdertDato
                    )
                )
            )
        }
    }


    @Test
    fun `skal overskrive ut mellomlagret vurdering fra API`() {
        val ds = MockDataSource()
        val behandling = opprettBehandling(nySak(), TypeBehandling.Revurdering)
        val avklaringsbehovKode = AvklaringsbehovKode.`5001`

        val mellomlagretVurdering = MellomlagretVurdering(
            behandlingId = behandling.id,
            avklaringsbehovKode = avklaringsbehovKode,
            data = """
                    {"element": "verdi", "tallElement": 1234, "boolskElement": true}
                    """.trimIndent(),
            vurdertAv = "A123456",
            vurdertDato = LocalDateTime.now().withNano(0)
        )
        InMemoryMellomlagretVurderingRepository.lagre(mellomlagretVurdering)

        testApplication {
            installApplication {
                mellomlagretVurderingApi(ds, inMemoryRepositoryRegistry)
            }

            val nyMellomlagretVurdering = MellomlagretVurderingRequest(
                behandlingsReferanse = behandling.referanse.referanse,
                avklaringsbehovkode = avklaringsbehovKode.name,
                data = "{}",
            )
            val response =
                createClient().post("/api/behandling/mellomlagret-vurdering") {
                    header("Authorization", "Bearer ${getToken().token()}")
                    contentType(ContentType.Application.Json)
                    setBody(nyMellomlagretVurdering)
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            val resBody = response.body<MellomlagretVurderingResponse>()
            assertThat(resBody.mellomlagretVurdering?.data).isEqualTo(nyMellomlagretVurdering.data)

            val oppdatertVerdi =
                InMemoryMellomlagretVurderingRepository.hentHvisEksisterer(behandling.id, avklaringsbehovKode)
            assertThat(oppdatertVerdi).isNotNull
            assertThat(oppdatertVerdi!!.data).isEqualTo(nyMellomlagretVurdering.data)
        }
    }


    @Test
    fun `skal få tom verdi dersom man prøver å hente ut uten at det finnes noe mellomlagret verdi`() {
        val ds = MockDataSource()
        val behandling = opprettBehandling(nySak(), TypeBehandling.Revurdering)
        val avklaringsbehovKode = AvklaringsbehovKode.`8001`
        testApplication {
            installApplication {
                mellomlagretVurderingApi(ds, inMemoryRepositoryRegistry)
            }

            val response =
                createClient().get("/api/behandling/mellomlagret-vurdering/${behandling.referanse.referanse}/${avklaringsbehovKode}") {
                    header("Authorization", "Bearer ${getToken().token()}")
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            assertThat(response.body<MellomlagretVurderingResponse>()).isEqualTo(
                MellomlagretVurderingResponse(
                    mellomlagretVurdering = null
                )
            )
        }
    }

    @Test
    fun `skal slette mellomlagret vurdering fra API`() {
        val ds = MockDataSource()
        val behandling = opprettBehandling(nySak(), TypeBehandling.Revurdering)
        val avklaringsbehovKode = AvklaringsbehovKode.`5001`

        val mellomlagretVurdering = MellomlagretVurdering(
            behandlingId = behandling.id,
            avklaringsbehovKode = avklaringsbehovKode,
            data = """
                    {"element": "verdi", "tallElement": 1234, "boolskElement": true}
                    """.trimIndent(),
            vurdertAv = "A123456",
            vurdertDato = LocalDateTime.now().withNano(0)
        )
        InMemoryMellomlagretVurderingRepository.lagre(mellomlagretVurdering)

        testApplication {
            installApplication {
                mellomlagretVurderingApi(ds, inMemoryRepositoryRegistry)
            }


            val initiellVerdi =
                InMemoryMellomlagretVurderingRepository.hentHvisEksisterer(behandling.id, avklaringsbehovKode)
            assertThat(initiellVerdi).isNotNull

            val response =
                createClient().post("/api/behandling/mellomlagret-vurdering/${behandling.referanse.referanse}/${avklaringsbehovKode}/slett") {
                    header("Authorization", "Bearer ${getToken().token()}")
                    contentType(ContentType.Application.Json)
                }

            assertThat(response.status).isEqualTo(HttpStatusCode.Accepted)
            val oppdatertVerdi =
                InMemoryMellomlagretVurderingRepository.hentHvisEksisterer(behandling.id, avklaringsbehovKode)
            assertThat(oppdatertVerdi).isNull()
        }
    }

}