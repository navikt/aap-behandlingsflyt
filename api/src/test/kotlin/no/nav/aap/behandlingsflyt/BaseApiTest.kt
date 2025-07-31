package no.nav.aap.behandlingsflyt

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.inmemorygateway.FakeTilgangGateway
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.time.LocalDate
import java.util.UUID
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

open class BaseApiTest : BeforeAllCallback {

    companion object {
        @Volatile
        private var isStarted = false
        private val server = MockOAuth2Server()

    }

    fun opprettBehandling(sak: Sak, typeBehandling: TypeBehandling) =
        InMemoryBehandlingRepository.opprettBehandling(
            sak.id,
            vurderingsbehov = listOf(),
            typeBehandling = typeBehandling,
            forrigeBehandlingId = null,
        )

    fun nySak() = InMemorySakRepository.finnEllerOpprett(
        person = Person(
            identifikator = UUID.randomUUID(),
            identer = listOf(Ident("0".repeat(11)))
        ),
        periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
    )

    fun issueToken(scope: String) = server.issueToken(
        issuerId = "default",
        claims = mapOf(
            "scope" to scope,
            "consumer" to mapOf("authority" to "123", "ID" to "0192:889640782")
        ),
    )

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
            install(OpenAPIGen)
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                }
            }

            apiRouting(apiEndepunkt)
        }
    }

    override fun beforeAll(context: ExtensionContext?) {
        if (isStarted) {
            return
        }
        server.start()
        isStarted = true


        GatewayRegistry
            .register<FakeTilgangGateway>()
    }

}