package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.engine.*
import kotlinx.coroutines.runBlocking

abstract class FakeServer {
    protected abstract val server: EmbeddedServer<*, *>

    abstract fun start()

    fun port(): Int = runBlocking {
        server.engine.resolvedConnectors()
    }.first { it.type == ConnectorType.HTTP }.port

    fun stop() = server.stop(0L, 0L)
}
