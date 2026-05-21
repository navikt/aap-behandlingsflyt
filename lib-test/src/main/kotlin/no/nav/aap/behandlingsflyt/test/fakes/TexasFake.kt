package no.nav.aap.behandlingsflyt.test.fakes

import com.fasterxml.jackson.databind.JsonNode
import com.nimbusds.jwt.JWTParser
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.test.AzureTokenGen

@Suppress("PropertyName", "ConstructorParameterNaming")
data class TestToken(
    val access_token: String,
    val token_type: String = "token-type",
    val expires_in: Int = 3599,
)

class TexasFake : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        install(ContentNegotiation) {
            jackson()
        }
        installerStatusPages("TEXAS")
        routing {
            post("/token") {
                val token = AzureTokenGen("behandlingsflyt")
                    .generate(isApp = true, azp = "behandlingsflyt")
                call.respond(TestToken(access_token = token))
            }

            post("/token/exchange") {
                val body = call.receive<JsonNode>()
                val NAVident = JWTParser.parse(body["user_token"].asText())
                    .jwtClaimsSet
                    .getClaimAsString("NAVident")

                val token = AzureTokenGen(body["target"].asText())
                    .generate(isApp = false, azp = "behandlingsflyt", navIdent = NAVident)
                call.respond(TestToken(access_token = token))
            }

            post("/introspect") {
                call.respond(mapOf("active" to true))
            }
        }
    }
}
