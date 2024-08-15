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
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.flyt.internals.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.flyt.internals.TestHendelsesMottak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.institusjon.Institusjonsopphold
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TestApp {
    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_APP", matches = "RUN")
    fun `kjør test-app`() {
        // For å kjøre test-app fra kommandolinjen, kjør
        // cd app && TEST_APP=RUN ../gradlew test --tests TestApp --info
        main()
    }
}

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
        // Useful for connecting to the test database locally
        // jdbc URL contains the host and port and database name.
        println("jdbcUrl: ${postgres.jdbcUrl}. Password: ${postgres.password}. Username: ${postgres.username}.")
        server(
            dbConfig
        )
        module(fakes)

        val datasource = initDatasource(dbConfig)

        apiRouting {
            route("/test") {
                route("/opprett") {
                    post<Unit, OpprettTestcaseDTO, OpprettTestcaseDTO> { _, dto ->
                        val ident = genererIdent(dto.fødselsdato)
                        val barn = dto.barn.map { genererBarn(it) }
                        barn.forEach { fakes.leggTil(it) }
                        fakes.leggTil(
                            TestPerson(
                                identer = setOf(ident),
                                fødselsdato = Fødselsdato(dto.fødselsdato),
                                yrkesskade = if (dto.yrkesskade) listOf(TestYrkesskade()) else emptyList(),
                                uføre = dto.uføre?.let(::Prosent) ?: Prosent.`0_PROSENT`,
                                barn = barn,
                                institusjonsopphold = listOfNotNull(
                                    if (dto.institusjoner.fengsel == true) genererFengselsopphold() else null,
                                    if (dto.institusjoner.sykehus == true) genererSykehusopphold() else null,
                                )
                            )
                        )
                        val periode = Periode(
                            LocalDate.now(),
                            LocalDate.now().plusYears(3)
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

private fun genererFengselsopphold() = Institusjonsopphold(
    organisasjonsnummer = "12345",
    kategori = Oppholdstype.S.name,
    institusjonstype = Institusjonstype.FO.name,
    forventetSluttdato = LocalDate.now().plusYears(1),
    startdato = LocalDate.now().minusYears(2),
    institusjonsnavn = "Azkaban"
)

private fun genererSykehusopphold() = Institusjonsopphold(
    organisasjonsnummer = "12345",
    kategori = Oppholdstype.H.name,
    institusjonstype = Institusjonstype.HS.name,
    forventetSluttdato = LocalDate.now().plusYears(1),
    startdato = LocalDate.now().minusYears(2),
    institusjonsnavn = "St. Mungos Hospital"
)

private fun genererBarn(dto: TestBarn): TestPerson {
    val ident = genererIdent(dto.fodselsdato)

    return TestPerson(
        identer = setOf(ident),
        fødselsdato = Fødselsdato(dto.fodselsdato),
        yrkesskade = emptyList()
    )
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
    val postgres = PostgreSQLContainer<Nothing>("postgres:15")
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
