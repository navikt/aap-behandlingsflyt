package no.nav.aap.behandlingsflyt

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
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
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.arbeidsevne.FastsettArbeidsevneLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.bistand.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.meldeplikt.FritakMeldepliktLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.student.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.avklaringsbehov.flate.avklaringsbehovApi
import no.nav.aap.behandlingsflyt.behandling.flate.behandlingApi
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.flate.bistandsgrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.medlemskap.medlemskapsgrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt.flate.meldepliktsgrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.adapter.PersonRegisterMock
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.flate.studentgrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.flate.sykdomsgrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.vedtak.flate.fatteVedtakGrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.adapter.YrkesskadeRegisterMock
import no.nav.aap.behandlingsflyt.flyt.flate.flytApi
import no.nav.aap.behandlingsflyt.hendelse.mottak.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.hendelse.mottak.HendelsesMottak
import no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.søknad.Søknad
import no.nav.aap.behandlingsflyt.prosessering.Motor
import no.nav.aap.behandlingsflyt.prosessering.retry.RetryService
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.flate.saksApi
import org.flywaydb.core.Flyway
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

private val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")

class App

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> SECURE_LOGGER.error("Uhåndtert feil", e) }
    embeddedServer(Netty, port = 8080) { server(DbConfig()) }.start(wait = true)
}

internal fun Application.server(dbConfig: DbConfig) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) { registry = prometheus }
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            registerSubtypes(
                // TODO: Dette bør skje via reflection elns så dette ikke blir manuelt vedlikehold
                AvklarStudentLøsning::class.java,
                AvklarYrkesskadeLøsning::class.java,
                AvklarSykdomLøsning::class.java,
                AvklarSykepengerErstatningLøsning::class.java,
                AvklarBistandsbehovLøsning::class.java,
                FritakMeldepliktLøsning::class.java,
                FastsettArbeidsevneLøsning::class.java,
                ForeslåVedtakLøsning::class.java,
                FatteVedtakLøsning::class.java
            )
        }
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
    install(OpenAPIGen) {
        // this servers OpenAPI definition on /openapi.json
        serveOpenApiJson = true
        // this servers Swagger UI on /swagger-ui/index.html
        serveSwaggerUi = true
        info {
            title = "AAP - Saksbehandling"
        }
    }
    install(CORS) {
        anyHost() // FIXME: Dette blir litt vel aggresivt, men greit for nå? :pray:
        allowHeader(HttpHeaders.ContentType)
    }

    val dataSource = initDatasource(dbConfig)
    migrate(dataSource)
    apiRouting {
        configApi()
        saksApi(dataSource)
        behandlingApi(dataSource)
        flytApi(dataSource)
        fatteVedtakGrunnlagApi(dataSource)
        bistandsgrunnlagApi(dataSource)
        meldepliktsgrunnlagApi(dataSource)
        medlemskapsgrunnlagApi(dataSource)
        studentgrunnlagApi(dataSource)
        sykdomsgrunnlagApi(dataSource)
        avklaringsbehovApi(dataSource)

        hendelsesApi(dataSource)
        routing {
            actuator(prometheus)
        }
    }
    module(dataSource)
}

fun Application.module(dataSource: DataSource) {
    val motor = Motor(dataSource)

    dataSource.transaction {
        RetryService(it).enable()
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

@Deprecated("Kun for test lokalt enn så lenge")
fun NormalOpenAPIRoute.hendelsesApi(dataSource: DataSource) {
    route("/test/opprett") {
        post<Unit, OpprettTestcaseDTO, OpprettTestcaseDTO> { _, dto ->

            val ident = Ident(dto.ident)
            PersonRegisterMock.konstruer(ident, Personopplysning(Fødselsdato(dto.fødselsdato)))
            val periode = Periode(
                LocalDate.now().minusYears(3),
                LocalDate.now().plusYears(3)
            )
            if (dto.yrkesskade) {
                YrkesskadeRegisterMock.konstruer(
                    ident,
                    periode
                )
            }

            HendelsesMottak(dataSource).håndtere(
                ident, DokumentMottattPersonHendelse(
                    journalpost = JournalpostId("" + System.currentTimeMillis()),
                    mottattTidspunkt = LocalDateTime.now(),
                    strukturertDokument = StrukturertDokument(Søknad(periode), Brevkode.SØKNAD)
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

fun migrate(dataSource: DataSource) {
    val flyway = Flyway
        .configure()
        .cleanDisabled(false) // TODO: husk å skru av denne før prod
        .cleanOnValidationError(true) // TODO: husk å skru av denne før prod
        .dataSource(dataSource)
        .locations("flyway")
        .validateMigrationNaming(true)
        .load()

    flyway.migrate()
}
