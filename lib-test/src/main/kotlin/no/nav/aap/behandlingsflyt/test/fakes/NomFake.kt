package no.nav.aap.behandlingsflyt.test.fakes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomData
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomDataRessurs
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomRessursResponse
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomRessursVisningsnavn
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomRessurserVisningsnavn
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.OrgEnhet
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.OrgTilknytning
import no.nav.aap.behandlingsflyt.integrasjon.util.GraphQLResponse
import no.nav.aap.behandlingsflyt.test.TestBruker
import java.time.LocalDate

class NomFake : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        installerStatusPages("NOM")
        routing {
            post("/graphql") {
                val jsonNode = call.receive<JsonNode>()
                val data = if (jsonNode["query"].asText().contains("ressurser")) {
                    NomRessurserVisningsnavn(
                        ressurser = listOf(
                            NomRessursResponse(NomRessursVisningsnavn("ABC1245", "Sak Behandlersen")),
                            NomRessursResponse(NomRessursVisningsnavn("DEFG123", "Annen Testesen")),
                        )
                    )
                } else {
                    val navIdent = jsonNode["variables"]["navIdent"].asText()

                    NomData(
                        NomDataRessurs(
                            orgTilknytning = listOf(
                                OrgTilknytning(
                                    OrgEnhet("1234"),
                                    true,
                                    LocalDate.now(),
                                    null
                                )
                            ), visningsnavn = when (navIdent) {
                                TestBruker.VEILDER.ident -> "Veileder Etternavn"
                                TestBruker.KVALITETSSIKRER.ident -> "Kvalitetssikrer Etternavn"
                                TestBruker.SAKSBEHANDLER.ident -> "Saksbehandler Etternavn"
                                TestBruker.BESLUTTER.ident -> "Beslutter Etternavn"
                                TestBruker.SUPERBRUKER.ident -> "Superbruker Etternavn"
                                else -> "Ukjent Bruker"
                            }
                        )
                    )
                }
                val response = GraphQLResponse(
                    data,
                    emptyList()
                )
                call.respond(response)
            }
        }
    }
}
