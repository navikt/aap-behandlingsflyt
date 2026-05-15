package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.intellij.lang.annotations.Language

class AaregFake : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() {
        server.start()
    }

    private fun module(): Application.() -> Unit = {
        install(ContentNegotiation) {
            jackson()
        }
        installerStatusPages("AAREG")
        routing {
            post("/api/v2/arbeidstaker/arbeidsforhold") {
                call.respond(aaregResponse)
            }
        }
    }

    companion object {
        @Language("JSON")
        private val aaregResponse = """
            [
              {
                "type": {
                  "kode": "ordinaertArbeidsforhold",
                  "beskrivelse": "Ordinært arbeidsforhold"
                },
                "arbeidstaker": {
                  "identer": [
                    {
                      "type": "AKTORID",
                      "ident": "2336200023418",
                      "gjeldende": true
                    },
                    {
                      "type": "FOLKEREGISTERIDENT",
                      "ident": "244991*****",
                      "gjeldende": true
                    }
                  ]
                },
                "arbeidssted": {
                  "type": "Underenhet",
                  "identer": [
                    {
                      "type": "ORGANISASJONSNUMMER",
                      "ident": "896929119",
                      "gjeldende": null
                    }
                  ]
                },
                "opplysningspliktig": {
                  "type": "Hovedenhet",
                  "identer": [
                    {
                      "type": "ORGANISASJONSNUMMER",
                      "ident": "963743254",
                      "gjeldende": null
                    }
                  ]
                },
                "ansettelsesperiode": {
                  "startdato": "2005-01-16",
                  "sluttdato": null
                },
                "ansettelsesdetaljer": [
                  {
                    "type": "Ordinaer",
                    "yrke": {
                      "kode": "7125102",
                      "beskrivelse": "BYGNINGSSNEKKER"
                    },
                    "fartsomraade": {
                      "kode": "innenriks",
                      "beskrivelse": "Innenriks"
                    },
                    "skipsregister": {
                      "kode": "nor",
                      "beskrivelse": "NOR"
                    },
                    "fartoeystype": {
                      "kode": "Gigaskip",
                      "beskrivelse": "meget stort skip"
                    }
                  }
                ]
              }
            ]
        """.trimIndent()
    }
}
