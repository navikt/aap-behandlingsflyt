package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.aap.behandlingsflyt.auth.AZURE
import no.nav.aap.behandlingsflyt.auth.Bruker
import no.nav.aap.behandlingsflyt.auth.authentication
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.flate.avklaringsbehovApi
import no.nav.aap.behandlingsflyt.avklaringsbehov.flate.fatteVedtakGrunnlagApi
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.utledSubtypes
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbflyway.Migrering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad.Søknad
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.flate.beregningVurderingAPI
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.bistandsgrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.medlemskap.medlemskapsgrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.meldepliktsgrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.flate.studentgrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.sykdomsgrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.sykepengerGrunnlagApi
import no.nav.aap.behandlingsflyt.flyt.flate.behandlingApi
import no.nav.aap.behandlingsflyt.flyt.flate.flytApi
import no.nav.aap.behandlingsflyt.flyt.flate.søknadApi
import no.nav.aap.behandlingsflyt.hendelse.mottak.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.hendelse.mottak.HendelsesMottak
import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsOppgaver
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.saksApi
import no.nav.aap.behandlingsflyt.server.apiRoute
import no.nav.aap.behandlingsflyt.tilkjentytelse.flate.tilkjentYtelseAPI
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.motor.Motor
import no.nav.aap.motor.retry.RetryService
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

private val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")

class App

val SYSTEMBRUKER = Bruker("Kelvin")

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> SECURE_LOGGER.error("Uhåndtert feil", e) }
    embeddedServer(Netty, port = 8080) { server(DbConfig()) }.start(wait = true)
}

internal fun Application.server(dbConfig: DbConfig) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    DefaultJsonMapper.objectMapper()
        .registerSubtypes(utledSubtypes())

    install(MicrometerMetrics) { registry = prometheus }
    install(OpenAPIGen) {
        // this servers OpenAPI definition on /openapi.json
        serveOpenApiJson = true
        // this servers Swagger UI on /swagger-ui/index.html
        serveSwaggerUi = true
        info {
            title = "AAP - Saksbehandling"
        }
    }
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper = DefaultJsonMapper.objectMapper(), true))
    }
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XCorrelationId)
        generate { UUID.randomUUID().toString() }
    }
    install(CallLogging) {
        callIdMdc("callId")
        filter { call -> call.request.path().startsWith("/actuator").not() }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            if (cause is ElementNotFoundException) {
                call.respondText(status = HttpStatusCode.NotFound, text = "")
            } else {
                LoggerFactory.getLogger(App::class.java)
                    .info("Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
            }
        }
    }

    install(CORS) {
        anyHost() // FIXME: Dette blir litt vel aggresivt, men greit for nå? :pray:
        allowHeader(HttpHeaders.ContentType)
    }

    authentication(AzureConfig())

    val dataSource = initDatasource(dbConfig)
    Migrering.migrate(dataSource)

    routing {
        // Setter optional enn så lenge
        authenticate(AZURE, optional = true) {
            apiRoute {
                configApi()
                saksApi(dataSource)
                søknadApi(dataSource)
                behandlingApi(dataSource)
                flytApi(dataSource)
                fatteVedtakGrunnlagApi(dataSource)
                bistandsgrunnlagApi(dataSource)
                meldepliktsgrunnlagApi(dataSource)
                medlemskapsgrunnlagApi(dataSource)
                studentgrunnlagApi(dataSource)
                sykdomsgrunnlagApi(dataSource)
                sykepengerGrunnlagApi(dataSource)
                avklaringsbehovApi(dataSource)
                tilkjentYtelseAPI(dataSource)
                beregningVurderingAPI(dataSource)

                hendelsesApi(dataSource)
            }
        }
        actuator(prometheus)
        testIntegrasjoner()
    }
    module(dataSource)
}

fun Application.module(dataSource: DataSource) {
    val motor = Motor(dataSource = dataSource, antallKammer = 5, oppgaver = ProsesseringsOppgaver.alle())

    dataSource.transaction { dbConnection ->
        RetryService(dbConnection).enable()
    }

    environment.monitor.subscribe(ApplicationStarted) {
        motor.start()
    }
    environment.monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        motor.stop()
        // Release resources and unsubscribe from events
        application.environment.monitor.unsubscribe(ApplicationStarted) {}
        application.environment.monitor.unsubscribe(ApplicationStopped) {}
    }
}

fun NormalOpenAPIRoute.configApi() {
    route("/config/definisjoner") {
        get<Unit, List<Definisjon>> {
            respond(Definisjon.entries.toList())
        }
    }
}

private fun Routing.actuator(prometheus: PrometheusMeterRegistry) {
    route("/actuator") {
        get("/metrics") {
            call.respond(prometheus.scrape())
        }

        get("/live") {
            val status = HttpStatusCode.OK
            call.respond(status, "Oppe!")
        }

        get("/ready") {
            val status = HttpStatusCode.OK
            call.respond(status, "Oppe!")
        }
    }
}

@Deprecated("Test av integrasjoner")
fun Route.testIntegrasjoner() {
    route("/test/pdl") {
        get {
            val ident = call.request.header("personident")
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Mangler header personident")

            val identer = PdlIdentGateway.hentAlleIdenterForPerson(Ident(ident))
            call.respond(identer)
        }
    }
}

@Deprecated("Kun for test lokalt enn så lenge")
fun NormalOpenAPIRoute.hendelsesApi(dataSource: DataSource) {
    route("/test/opprett") {
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


            HendelsesMottak(dataSource).håndtere(
                ident, DokumentMottattPersonHendelse(
                    journalpost = JournalpostId("" + System.currentTimeMillis()),
                    mottattTidspunkt = LocalDateTime.now(),
                    strukturertDokument = StrukturertDokument(Søknad(periode, dto.student), Brevkode.SØKNAD)
                )
            )
            respond(dto)
        }
    }
    route("/test/pliktkort") {
        post<Unit, PliktkortTestDTO, PliktkortTestDTO> { _, dto ->

            val ident = Ident(dto.ident)

            HendelsesMottak(dataSource).håndtere(
                ident, DokumentMottattPersonHendelse(
                    journalpost = JournalpostId("" + System.currentTimeMillis()),
                    mottattTidspunkt = LocalDateTime.now(),
                    strukturertDokument = StrukturertDokument(dto.pliktkort, Brevkode.PLIKTKORT)
                )
            )
            respond(dto)
        }
    }
}

class DbConfig(
    val host: String = System.getenv("NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_HOST"),
    val port: String = System.getenv("NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_PORT"),
    val database: String = System.getenv("NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_DATABASE"),
    val url: String = "jdbc:postgresql://$host:$port/$database",
    val username: String = System.getenv("NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_USERNAME"),
    val password: String = System.getenv("NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_PASSWORD")
)

fun initDatasource(dbConfig: DbConfig) = HikariDataSource(HikariConfig().apply {
    jdbcUrl = dbConfig.url
    username = dbConfig.username
    password = dbConfig.password
    maximumPoolSize = 10
    minimumIdle = 1
    driverClassName = "org.postgresql.Driver"
})
