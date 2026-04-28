package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.intellij.lang.annotations.Language

class EregFake : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("EREG")
        routing {
            get("/api/v2/organisasjon/{orgnummer}") {
                call.respond(eregResponse)
            }
        }
    }

    companion object {
        @Language("JSON")
        private val eregResponse = """
            {
              "organisasjonsnummer": "990983666",
              "type": "Virksomhet",
              "navn": {
                "sammensattnavn": "NAV IKT",
                "navnelinje1": "NAV IKT",
                "bruksperiode": {
                  "fom": "2015-02-23T08:04:53.2"
                },
                "gyldighetsperiode": {
                  "fom": "2010-04-09"
                }
              },
              "organisasjonDetaljer": {
                "registreringsdato": "2007-03-05T00:00:00",
                "enhetstyper": [
                  {
                    "enhetstype": "BEDR",
                    "bruksperiode": {
                      "fom": "2014-05-21T15:46:47.225"
                    },
                    "gyldighetsperiode": {
                      "fom": "2007-03-05"
                    }
                  }
                ],
                "navn": [
                  {
                    "sammensattnavn": "NAV IKT",
                    "navnelinje1": "NAV IKT",
                    "bruksperiode": {
                      "fom": "2015-02-23T08:04:53.2"
                    },
                    "gyldighetsperiode": {
                      "fom": "2010-04-09"
                    }
                  }
                ],
                "naeringer": [
                  {
                    "naeringskode": "84.300",
                    "hjelpeenhet": false,
                    "bruksperiode": {
                      "fom": "2014-05-22T01:18:10.661"
                    },
                    "gyldighetsperiode": {
                      "fom": "2006-07-01"
                    }
                  }
                ],
                "forretningsadresser": [
                  {
                    "type": "Forretningsadresse",
                    "adresselinje1": "Sannergata 2",
                    "postnummer": "0557",
                    "landkode": "NO",
                    "kommunenummer": "0301",
                    "bruksperiode": {
                      "fom": "2015-02-23T10:38:34.403"
                    },
                    "gyldighetsperiode": {
                      "fom": "2007-08-23"
                    }
                  }
                ],
                "postadresser": [
                  {
                    "type": "Postadresse",
                    "adresselinje1": "Postboks 5 St Olavs plass",
                    "postnummer": "0130",
                    "landkode": "NO",
                    "kommunenummer": "0301",
                    "bruksperiode": {
                      "fom": "2015-02-23T10:38:34.403"
                    },
                    "gyldighetsperiode": {
                      "fom": "2010-10-08"
                    }
                  }
                ],
                "navSpesifikkInformasjon": {
                  "erIA": true,
                  "bruksperiode": {
                    "fom": "2015-01-27T16:01:18.562"
                  },
                  "gyldighetsperiode": {
                    "fom": "2015-01-27"
                  }
                },
                "sistEndret": "2014-02-17"
              },
              "virksomhetDetaljer": {
                "enhetstype": "BEDR",
                "oppstartsdato": "2006-07-01"
              }
            }
        """.trimIndent()
    }
}
