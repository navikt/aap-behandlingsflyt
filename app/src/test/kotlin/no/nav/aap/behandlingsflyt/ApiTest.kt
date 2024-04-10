package no.nav.aap.behandlingsflyt

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.flyt.flate.SøknadDto
import no.nav.aap.behandlingsflyt.flyt.flate.SøknadSendDto
import no.nav.aap.behandlingsflyt.flyt.flate.SøknadStudentDto
import no.nav.aap.behandlingsflyt.flyt.flate.VilkårDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.FinnEllerOpprettSakDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksinfoDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.UtvidetSaksinfoDTO
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.request.GetRequest
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class ApiTest {
    companion object {
        private val postgres = postgreSQLContainer()
        private val fakes = Fakes()

        // Starter server
        private val server = embeddedServer(Netty, port = 8080) {
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
        }.start()

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.stop()
            fakes.close()
            postgres.close()
        }
    }

    @Disabled
    @Test
    fun test() {
        fakes.leggTil(
            TestPerson(
                identer = setOf(Ident("12345678910")),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                yrkesskade = emptyList()
            )
        )

        val client = RestClient(
            config = ClientConfig(),
            tokenProvider = NoTokenTokenProvider()
        )
        val responseSak: SaksinfoDTO? = client.post(
            URI.create("http://localhost:8080/").resolve("api/sak/finnEllerOpprett"),
            PostRequest(
                body = FinnEllerOpprettSakDTO("12345678910", LocalDate.now()),
                responseClazz = SaksinfoDTO::class.java
            )
        )

        requireNotNull(responseSak)

        client.post(
            URI.create("http://localhost:8080/").resolve("api/soknad/send"),
            PostRequest(
                body = SøknadSendDto(responseSak.saksnummer, "123", SøknadDto(SøknadStudentDto("NEI"))),
                responseClazz = Unit::class.java
            )
        )

        val utvidetSak: UtvidetSaksinfoDTO? = client.get(
            URI.create("http://localhost:8080/").resolve("api/sak/").resolve(responseSak.saksnummer),
            GetRequest(responseClazz = UtvidetSaksinfoDTO::class.java)
        )

        requireNotNull(utvidetSak)

        Thread.sleep(Duration.ofSeconds(10))

        data class EndringDTO(
            val status: no.nav.aap.behandlingsflyt.avklaringsbehov.Status,
            val tidsstempel: LocalDateTime = LocalDateTime.now(),
            val begrunnelse: String,
            val endretAv: String
        )

        data class AvklaringsbehovDTO(
            val definisjon: Any,
            val status: no.nav.aap.behandlingsflyt.avklaringsbehov.Status,
            val endringer: List<EndringDTO>
        )

        data class DetaljertBehandlingDTO(
            val referanse: UUID,
            val type: String,
            val status: no.nav.aap.verdityper.sakogbehandling.Status,
            val opprettet: LocalDateTime,
            val avklaringsbehov: List<AvklaringsbehovDTO>,
            val vilkår: List<VilkårDTO>,
            val aktivtSteg: StegType,
            val versjon: Long
        )

        val behandling: DetaljertBehandlingDTO? = client.get(
            URI.create("http://localhost:8080/")
                .resolve("api/behandling/")
                .resolve(utvidetSak.behandlinger.first().referanse.toString()),
            GetRequest(responseClazz = DetaljertBehandlingDTO::class.java)
        )

        println(behandling)

        assertThat(true).isTrue
    }
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
