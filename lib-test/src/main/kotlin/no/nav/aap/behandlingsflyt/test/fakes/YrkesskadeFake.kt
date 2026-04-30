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
import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.SkadekombinasjonModell
import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.YrkesskadeModell
import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.YrkesskadeRequest
import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.test.TestPersonService
import java.time.LocalDate

class YrkesskadeFake(private val fakePersoner: () -> TestPersonService) : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        installerStatusPages("YRKESSKADE")
        routing {
            post("/api/v1/saker/") {
                val req = call.receive<YrkesskadeRequest>()
                val person = req.foedselsnumre.map { hentEllerGenererTestPerson(fakePersoner(), it) }

                call.respond(
                    Yrkesskader(
                        saker = person.flatMap { it.yrkesskade }
                            .map {
                                YrkesskadeModell(
                                    kommunenr = "1234",
                                    saksblokk = "A",
                                    saksnr = (10000000..99999999).random(),
                                    sakstype = "Yrkesskade",
                                    mottattdato = LocalDate.now().minusDays(25),
                                    resultat = "GODKJENT",
                                    resultattekst = "Godkjent",
                                    vedtaksdato = LocalDate.now().minusDays(8),
                                    skadeart = it.skadeart ?: "arbeidsulykke",
                                    diagnose = it.diagnose ?: "bruddskade",
                                    skadedato = it.skadedato,
                                    kildetabell = "",
                                    kildesystem = listOf("Kompys", "Infotrygd").random(),
                                    eksternreferanser = null,
                                    saksreferanse = it.saksreferanse,
                                    skadekombinasjoner = it.skadebeskrivelse?.let { beskrivelse ->
                                        val deler = beskrivelse.split(" i ", ignoreCase = true, limit = 2)
                                        listOf(
                                            SkadekombinasjonModell(
                                                kroppsdel = deler.getOrNull(1)?.trim()?.lowercase() ?: "skulder",
                                                skadetype = deler.getOrNull(0)?.trim()?.lowercase() ?: "dislokasjon"
                                            )
                                        )
                                    },
                                    skadekombinasjonerTekst = it.skadebeskrivelse,
                                    saksbehandlingsansvarligIdent = null
                                )
                            }
                    )
                )
            }
        }
    }
}
