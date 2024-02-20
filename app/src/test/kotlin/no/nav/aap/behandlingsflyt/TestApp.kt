package no.nav.aap.behandlingsflyt

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.behandlingsflyt.test.Fakes
import org.testcontainers.containers.PostgreSQLContainer

// Kjøres opp for å få logback i console uten json
fun main() {
    val postgres = postgreSQLContainer()



    // Starter server
    embeddedServer(Netty, port = 8080) {
        server(
            DbConfig(
                host = "sdg",
                port = "sdf",
                database = "sdf",
                url = postgres.jdbcUrl,
                username = postgres.username,
                password = postgres.password
            )
        )
        module()
    }.start(wait = true)
}

private fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
    postgres.start()
    Thread.sleep(10000); // Trengs denne virkelig?
    return postgres
}

fun Application.module() {
    // Setter opp virtuell sandkasse lokalt
    val fakes = Fakes()

    environment.monitor.subscribe(ApplicationStarted) {
    }
    environment.monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        fakes.close()
        // Release resources and unsubscribe from events
        application.environment.monitor.unsubscribe(ApplicationStarted) {}
        application.environment.monitor.unsubscribe(ApplicationStopped) {}
    }
}
