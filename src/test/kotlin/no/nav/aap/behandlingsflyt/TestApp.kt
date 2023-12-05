package no.nav.aap.behandlingsflyt

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.testcontainers.containers.PostgreSQLContainer

// Kjøres opp for å få logback i console uten json
fun main() {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
    postgres.start()
    Thread.sleep(10000);
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
    }.start(wait = true)
}
