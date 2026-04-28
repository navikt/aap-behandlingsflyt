package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.test.modell.MockUnleashFeature
import no.nav.aap.behandlingsflyt.test.modell.MockUnleashFeatures
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.komponenter.json.DefaultJsonMapper

class UnleashFake : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper = DefaultJsonMapper.objectMapper(), true)
            )
        }
        installerStatusPages("UNLEASH")
        routing {
            get("/api/client/features") {
                val features = BehandlingsflytFeature.entries.map { MockUnleashFeature(it.name, true) }
                val response = MockUnleashFeatures(features = features)
                call.respond(response)
            }
        }
    }
}
