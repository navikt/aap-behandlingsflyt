package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad.Søknad
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad.SøknadStudentDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.flyt.internals.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.flyt.internals.TestHendelsesMottak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.genererIdent
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

// Kjøres opp for å få logback i console uten json
fun main() {
    val postgres = postgreSQLContainer()
    val fakes = Fakes(azurePort = 8081)

    // Starter server
    embeddedServer(Netty, port = 8080) {
        val dbConfig = DbConfig(
            host = "sdg",
            port = "sdf",
            database = "sdf",
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password
        )
        server(
            dbConfig
        )
        module(fakes)

        val datasource = initDatasource(dbConfig)

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
            route("/test") {
                route("/opprett") {
                    post<Unit, OpprettTestcaseDTO, OpprettTestcaseDTO> { _, dto ->

                        val ident = Ident(dto.ident)
                        val periode = Periode(
                            LocalDate.now(),
                            LocalDate.now().plusYears(3)
                        )


                        val client = RestClient(
                            config = ClientConfig(),
                            tokenProvider = NoTokenTokenProvider()
                        )
                        client.post<_, Unit>(
                            URI.create("http://localhost:8080/").resolve("testdataApi/opprettPerson"),
                            PostRequest(body = dto)
                        )


                        TestHendelsesMottak(datasource).håndtere(
                            ident, DokumentMottattPersonHendelse(
                                journalpost = JournalpostId("" + System.currentTimeMillis()),
                                mottattTidspunkt = LocalDateTime.now(),
                                strukturertDokument = StrukturertDokument(
                                    mapTilSøknad(dto),
                                    Brevkode.SØKNAD
                                ),
                                periode = periode
                            )
                        )
                        respond(dto)
                    }
                }
                route("/pliktkort") {
                    post<Unit, PliktkortTestDTO, PliktkortTestDTO> { _, dto ->

                        val ident = Ident(dto.ident)

                        TestHendelsesMottak(datasource).håndtere(
                            ident, DokumentMottattPersonHendelse(
                                journalpost = JournalpostId("" + System.currentTimeMillis()),
                                mottattTidspunkt = LocalDateTime.now(),
                                strukturertDokument = StrukturertDokument(dto.pliktkort, Brevkode.PLIKTKORT),
                                periode = dto.pliktkort.periode()
                            )
                        )
                        respond(dto)
                    }
                }
            }
        }

    }.start(wait = true)
}

fun mapTilSøknad(dto: OpprettTestcaseDTO): Søknad {
    val erStudent = if (dto.student) {
        "JA"
    } else {
        "NEI"
    }
    val harYrkesskade = if (dto.yrkesskade) {
        "JA"
    } else {
        "NEI"
    }
    return Søknad(student = SøknadStudentDto(erStudent), harYrkesskade)
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
