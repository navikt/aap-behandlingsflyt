package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad.Søknad
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.hendelse.mottak.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.hendelse.mottak.HendelsesMottak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.modell.TestOpprettPerson
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestSøknad
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

// Kjøres opp for å få logback i console uten json
fun main() {
    val postgres = postgreSQLContainer()
    val fakes = Fakes()

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
            route("/testdataApi/opprettPerson"){
                post<Unit, OpprettTestcaseDTO, OpprettTestcaseDTO> { _, dto ->
                    fakes.leggTil(TestPerson(
                        identer = setOf(Ident(dto.ident)),
                        fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                        yrkesskade=if(dto.yrkesskade) listOf(TestYrkesskade()) else emptyList()
                    ))

                    respond(dto)
                }
            }
        }

    }.start(wait = true)
}

private fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
    postgres.start()
    Thread.sleep(10000); // Trengs denne virkelig?
    return postgres
}

fun Application.module(fakes: Fakes) {
    // Setter opp virtuell sandkasse lokalt


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
