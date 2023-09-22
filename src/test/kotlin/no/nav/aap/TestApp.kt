package no.nav.aap

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

// Kjøres opp for å få logback i console uten json
fun main() {
    embeddedServer(Netty, port = 8080, module = Application::server).start(wait = true)
}