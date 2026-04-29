package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.behandling.beregning.Månedsinntekt
import no.nav.aap.behandlingsflyt.test.TestPersonService
import no.nav.aap.komponenter.verdityper.Beløp
import java.math.MathContext
import java.math.RoundingMode
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class AinntektFake(private val fakePersoner: () -> TestPersonService) : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())

    override fun start() {
        server.start()
    }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("A-INNTEKT")
        routing {
            post("/hentinntektliste") {
                val request = call.receive<Map<String, Any>>()
                val person = fakePersoner().hentPerson((request["ident"] as Map<*, *>)["identifikator"] as String)

                val z = person!!.inntekter().flatMap { inntektPerÅr ->
                    (1..12).map { mnd ->
                        Månedsinntekt(
                            YearMonth.of(inntektPerÅr.år.value, mnd),
                            Beløp(
                                inntektPerÅr.beløp.verdi.divide(
                                    12.toBigDecimal(),
                                    MathContext(10, RoundingMode.HALF_UP)
                                )
                            )
                        )
                    }
                }.joinToString(",") {
                    ainntektResponse(it.årMåned, it.beløp.verdi.toDouble())
                }
                call.respond(
                    """
                        {"arbeidsInntektMaaned": [$z]}
                    """.trimIndent()
                )
            }
        }
    }

    private fun ainntektResponse(årMnd: YearMonth, beløp: Double): String {
        val årMndFormatert = årMnd.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val opptjeningsperiode = årMnd.atDay(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return """
  
                    {
                        "aarMaaned": "$årMndFormatert",
                        "arbeidsInntektInformasjon": {
                            "inntektListe": [
                                {
                                    "beloep": $beløp,
                                    "opptjeningsland": null,
                                    "skattemessigBosattLand": null,
                                    "opptjeningsperiodeFom": "$opptjeningsperiode",
                                    "opptjeningsperiodeTom": null,
                                    "beskrivelse": "ikkeSykepenger",
                                    "virksomhet": {
                                      "identifikator": "896929119",
                                      "aktoerType": "AKTOER_ID"
                                    }
                                }
                            ]
                        }
                    }
        """.trimIndent()
    }
}
