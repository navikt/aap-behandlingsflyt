package no.nav.aap.behandlingsflyt.test.fakes

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.test.ErrorRespons
import no.nav.aap.behandlingsflyt.test.TestPersonService
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import org.slf4j.LoggerFactory
import java.time.LocalDate

private val logger = LoggerFactory.getLogger("FakeHelper")

internal fun Application.installerContentNegotiation() {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}

internal fun Application.installerStatusPages(moduleName: String) {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.info("$moduleName :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ErrorRespons(cause.message)
            )
        }
    }
}

internal fun hentEllerGenererTestPerson(fakePersoner: TestPersonService, forespurtIdent: String): TestPerson {
    val person = fakePersoner.hentPerson(forespurtIdent)
    if (person == null) {
        logger.info("Fant ikke testperson med ident $forespurtIdent.")
        fakePersoner.leggTil(
            TestPerson(
                identer = setOf(Ident(forespurtIdent)),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(30))
            )
        )
    }
    return fakePersoner.hentPerson(forespurtIdent)!!
}
