package no.nav.aap.behandlingsflyt

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemorygateway.FakeTilgangGateway
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@Fakes
class PersonApiTest : BaseApiTest() {

    @Test
    fun `returnerer aktiv ident for kjent person`() {
        val ident = "12345678901"
        val person = InMemoryPersonRepository.finnEllerOpprett(listOf(Ident(ident)))

        testApplication {
            installApplication {
                personApi(MockDataSource(), inMemoryRepositoryRegistry, createGatewayProvider {
                    register<FakeTilgangGateway>()
                })
            }

            val response = createClient().post("/api/person/ident") {
                header("Authorization", "Bearer ${getToken().token()}")
                contentType(ContentType.Application.Json)
                setBody(PersonIdentRequest(personReferanse = person.identifikator))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.body<PersonIdentResponse>()).isEqualTo(
                PersonIdentResponse(ident = ident)
            )
        }
    }

    @Test
    fun `returnerer korrekt ident blant flere identer`() {
        val aktivIdent = "12345678902"
        val person = InMemoryPersonRepository.finnEllerOpprett(
            listOf(
                Ident("98765432109", aktivIdent = false),
                Ident(aktivIdent),
            )
        )

        testApplication {
            installApplication {
                personApi(MockDataSource(), inMemoryRepositoryRegistry, createGatewayProvider {
                    register<FakeTilgangGateway>()
                })
            }

            val response = createClient().post("/api/person/ident") {
                header("Authorization", "Bearer ${getToken().token()}")
                contentType(ContentType.Application.Json)
                setBody(PersonIdentRequest(personReferanse = person.identifikator))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.body<PersonIdentResponse>()).isEqualTo(
                PersonIdentResponse(ident = aktivIdent)
            )
        }
    }
}
