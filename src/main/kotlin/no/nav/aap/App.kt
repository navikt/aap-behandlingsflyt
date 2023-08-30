package no.nav.aap

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.dsl.post
import io.github.smiley4.ktorswaggerui.dsl.route
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.aap.domene.behandling.grunnlag.person.Fødselsdato
import no.nav.aap.domene.behandling.grunnlag.person.PersonRegisterMock
import no.nav.aap.domene.behandling.grunnlag.person.Personinfo
import no.nav.aap.domene.behandling.grunnlag.yrkesskade.YrkesskadeRegisterMock
import no.nav.aap.domene.typer.Ident
import no.nav.aap.domene.typer.Periode
import no.nav.aap.flate.behandling.avklaringsbehov.avklaringsbehovApi
import no.nav.aap.flate.behandling.behandlingApi
import no.nav.aap.flate.sak.saksApi
import no.nav.aap.hendelse.mottak.DokumentMottattPersonHendelse
import no.nav.aap.hendelse.mottak.HendelsesMottak
import java.time.LocalDate

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
        }
    }
    install(CORS) {
        anyHost() // FIXME: Dette blir litt vel aggresivt, men greit for nå? :pray:
        allowHeader(HttpHeaders.ContentType)
    }
    install(SwaggerUI) {
        swagger {
            swaggerUrl = "swagger-ui"
            forwardRoot = true
        }
        info {
            title = "AAP - Saksbehandling"
            version = "latest"
            description = ""
        }
        server {
            url = "http://localhost:8080"
            description = ""
        }
    }

    routing {
        actuator(prometheus)
        saksApi()
        behandlingApi()
        avklaringsbehovApi()

        hendelsesApi()
    }
}

private fun Routing.actuator(prometheus: PrometheusMeterRegistry) {
    route("/actuator", {
        hidden = true
    }) {
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
fun Routing.hendelsesApi() {
    route("/test/opprett", {
        tags = listOf("test")
    }) {
        post({
            request { body<OpprettTestcaseDTO>() }
            response {
                HttpStatusCode.Created to {
                    description = "Opprettet testcase, søk opp via ident"
                }
            }
        }) {
            val dto = call.receive<OpprettTestcaseDTO>()

            val ident = Ident(dto.ident)
            PersonRegisterMock.konstruer(ident, Personinfo(Fødselsdato(dto.fødselsdato)))
            if (dto.yrkesskade) {
                YrkesskadeRegisterMock.konstruer(
                    ident,
                    Periode(LocalDate.now().minusYears(3), LocalDate.now().plusYears(3))
                )
            }

            HendelsesMottak.håndtere(
                ident,
                DokumentMottattPersonHendelse(Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
            )
            call.respond(HttpStatusCode.Created)
        }
    }
}
