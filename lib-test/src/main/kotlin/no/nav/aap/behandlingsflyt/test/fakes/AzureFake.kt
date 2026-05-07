package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.test.AZURE_JWKS
import no.nav.aap.behandlingsflyt.test.AzureTokenGen
import no.nav.aap.behandlingsflyt.test.TexasPortHolder

class AzureFake : FakeServer() {
    override val server = embeddedServer(Netty, port = TexasPortHolder.getPort(), module = module())
    override fun start() {
        server.start()
    }

    private fun module(): Application.() -> Unit = {
        install(ContentNegotiation) {
            jackson()
        }
        installerStatusPages("AZURE")
        routing {
            post("/token/{NAVident}") {
                val body = call.receiveText()
                val NAVident = call.parameters["NAVident"]
                val token = AzureTokenGen("behandlingsflyt")
                    .generate(body.contains("grant_type=client_credentials"), azp = "behandlingsflyt", NAVident)
                call.respond(TestToken(access_token = token))
            }
            get("/jwks") {
                call.respond(AZURE_JWKS)
            }
        }
    }
}
