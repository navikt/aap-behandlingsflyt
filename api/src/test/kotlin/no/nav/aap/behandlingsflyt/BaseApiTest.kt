package no.nav.aap.behandlingsflyt

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import io.ktor.serialization.jackson.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeServers
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.komponenter.type.Periode
import java.net.URI
import java.time.LocalDate
import java.util.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

open class BaseApiTest {

    fun opprettBehandling(sak: Sak, typeBehandling: TypeBehandling) =
        InMemoryBehandlingRepository.opprettBehandling(
            sak.id,
            vurderingsbehov = listOf(),
            typeBehandling = typeBehandling,
            forrigeBehandlingId = null,
        )

    fun nySak(søknadsDato: LocalDate = LocalDate.now()) = InMemorySakRepository.finnEllerOpprett(
        person = Person(
            identifikator = UUID.randomUUID(),
            identer = listOf(Ident("0".repeat(11)))
        ),
        periode = Periode(søknadsDato, søknadsDato.plusYears(1)),
    )

    fun getToken(): OidcToken {
        val client = RestClient(
            config = ClientConfig(scope = "behandlingsflyt"),
            tokenProvider = NoTokenTokenProvider(),
            responseHandler = DefaultResponseHandler()
        )
        return OidcToken(
            client.post<Unit, FakeServers.TestToken>(
                URI.create(requiredConfigForKey("azure.openid.config.token.endpoint")),
                PostRequest(Unit)
            )!!.access_token
        )
    }

    fun ApplicationTestBuilder.createClient() = createClient {
        install(ClientContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
    }

    fun ApplicationTestBuilder.installApplication(apiEndepunkt: NormalOpenAPIRoute.() -> Unit) {
        application {
            commonKtorModule(
                prometheus,
                AzureConfig(),
                InfoModel(
                    title = "AAP - Behandlingsflyt", version = "vTestApi",
                    description = "Api tester",
                )
            )

            routing {
                authenticate(AZURE) {
                    apiRouting(apiEndepunkt)

                }

            }
        }
    }
}