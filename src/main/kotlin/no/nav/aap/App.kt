package no.nav.aap

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
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
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.domene.ElementNotFoundException
import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.domene.behandling.grunnlag.person.Fødselsdato
import no.nav.aap.behandlingsflyt.domene.behandling.grunnlag.person.PersonRegisterMock
import no.nav.aap.behandlingsflyt.domene.behandling.grunnlag.person.Personinfo
import no.nav.aap.behandlingsflyt.domene.behandling.grunnlag.yrkesskade.YrkesskadeRegisterMock
import no.nav.aap.behandlingsflyt.domene.person.Ident
import no.nav.aap.behandlingsflyt.flate.behandling.avklaringsbehov.avklaringsbehovApi
import no.nav.aap.behandlingsflyt.flate.behandling.behandlingApi
import no.nav.aap.behandlingsflyt.flate.sak.saksApi
import no.nav.aap.flyt.ErrorRespons
import no.nav.aap.hendelse.mottak.DokumentMottattPersonHendelse
import no.nav.aap.hendelse.mottak.HendelsesMottak
import no.nav.aap.prosessering.Motor
import org.slf4j.LoggerFactory
import java.time.LocalDate


class App

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::server).start(wait = true)
}

internal fun Application.server() {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) { registry = prometheus }
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            registerSubtypes(
                AvklarSykdomLøsning::class.java, ForeslåVedtakLøsning::class.java, FatteVedtakLøsning::class.java
            )
        }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            if (cause is no.nav.aap.behandlingsflyt.domene.ElementNotFoundException) {
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

    apiRouting {
        configApi()
        saksApi()
        behandlingApi()
        avklaringsbehovApi()

        hendelsesApi()
        routing {
            actuator(prometheus)
        }
    }
    module()
}

fun Application.module() {
    environment.monitor.subscribe(ApplicationStarted) { application ->
        Motor.start()
    }
    environment.monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        Motor.stop()
        // Release resources and unsubscribe from events
        application.environment.monitor.unsubscribe(ApplicationStarted) {}
        application.environment.monitor.unsubscribe(ApplicationStopped) {}
    }
}

fun NormalOpenAPIRoute.configApi() {
    route("/config/definisjoner") {
        get<Unit, List<Definisjon>> { request ->
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
fun NormalOpenAPIRoute.hendelsesApi() {
    route("/test/opprett") {
        post<Unit, OpprettTestcaseDTO, OpprettTestcaseDTO> { path, dto ->

            val ident = Ident(dto.ident)
            PersonRegisterMock.konstruer(ident, Personinfo(Fødselsdato(dto.fødselsdato)))
            if (dto.yrkesskade) {
                YrkesskadeRegisterMock.konstruer(
                    ident,
                    no.nav.aap.behandlingsflyt.domene.Periode(
                        LocalDate.now().minusYears(3),
                        LocalDate.now().plusYears(3)
                    )
                )
            }

            HendelsesMottak.håndtere(
                ident, DokumentMottattPersonHendelse(
                    no.nav.aap.behandlingsflyt.domene.Periode(
                        LocalDate.now(),
                        LocalDate.now().plusYears(3)
                    )
                )
            )
            respond(dto)
        }
    }
}
