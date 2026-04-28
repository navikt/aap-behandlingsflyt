package no.nav.aap.behandlingsflyt.test.fakes

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.integrasjon.institusjonsopphold.InstitusjonoppholdRequest
import no.nav.aap.behandlingsflyt.test.TestPersonService

class Inst2Fake(private val fakePersoner: () -> TestPersonService) : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() {
        server.start()
    }

    private fun module(): Application.() -> Unit = {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        installerStatusPages("INST2")
        routing {
            post {
                val body = call.receive<InstitusjonoppholdRequest>()
                val ident = body.personident

                val fakePerson = fakePersoner().hentPerson(ident)

                if (fakePerson != null) {
                    call.respond(fakePerson.institusjonsopphold)
                } else {
                    call.respond<List<InstitusjonoppholdRequest>>(emptyList())
                }
            }
        }
    }
}
