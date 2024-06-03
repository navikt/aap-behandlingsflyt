package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.genererIdent
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.time.Duration
import java.time.temporal.ChronoUnit

// Kjøres opp for å få logback i console uten json
fun main() {
    val postgres = postgreSQLContainer()
    val fakes = Fakes(azurePort = 8081)

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
        module(fakes)

        apiRouting {
            route("/testdataApi/opprettPerson") {
                post<Unit, OpprettTestcaseDTO, OpprettTestcaseDTO> { _, dto ->
                    fakes.leggTil(
                        TestPerson(
                            identer = setOf(Ident(dto.ident)),
                            fødselsdato = Fødselsdato(dto.fødselsdato),
                            yrkesskade = if (dto.yrkesskade) listOf(TestYrkesskade()) else emptyList()
                        )
                    )

                    respond(dto)
                }
            }
            route("/testdataApi/genererPerson") {
                post<Unit, OpprettTestPersonResponsDto, OpprettTestPersonDto> { _, dto ->
                    val ident = genererIdent(dto.fødselsdato)
                    fakes.leggTil(
                        TestPerson(
                            identer = setOf(ident),
                            fødselsdato = Fødselsdato(dto.fødselsdato),
                            yrkesskade = if (dto.yrkesskade) listOf(TestYrkesskade()) else emptyList()
                        )
                    )

                    respond(OpprettTestPersonResponsDto(ident.identifikator))
                }
            }
        }

    }.start(wait = true)
}

private fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}

private fun Application.module(fakes: Fakes) {
    // Setter opp virtuell sandkasse lokalt
    environment.monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        fakes.close()
        // Release resources and unsubscribe from events
        application.environment.monitor.unsubscribe(ApplicationStopped) {}
    }
}
