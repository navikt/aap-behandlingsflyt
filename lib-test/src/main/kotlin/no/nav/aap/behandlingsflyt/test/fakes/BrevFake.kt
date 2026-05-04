package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.brev.kontrakt.AvbrytBrevbestillingRequest
import no.nav.aap.brev.kontrakt.BestillBrevResponse
import no.nav.aap.brev.kontrakt.BestillBrevV2Request
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.BrevdataDto
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.FerdigstillBrevRequest
import no.nav.aap.brev.kontrakt.HentSignaturerRequest
import no.nav.aap.brev.kontrakt.HentSignaturerResponse
import no.nav.aap.brev.kontrakt.Innhold
import no.nav.aap.brev.kontrakt.KanDistribuereBrevReponse
import no.nav.aap.brev.kontrakt.KanDistribuereBrevRequest
import no.nav.aap.brev.kontrakt.MottakerDistStatus
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.kontrakt.Tekstbolk
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.time.LocalDateTime
import java.util.*

class BrevFake : FakeServer() {
    private val brevStore = mutableMapOf<String, BrevbestillingResponse>()
    private val mutex = Any()

    override val server = embeddedServer(Netty, port = 0, module = module())

    override fun start() {
        server.start()
    }

    private fun module(): Application.() -> Unit = {
        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper = DefaultJsonMapper.objectMapper(), true)
            )
        }
        installerStatusPages("BREV")

        routing {
            route("/api") {
                post("/v2/bestill") {
                    val request = call.receive<BestillBrevV2Request>()
                    val eksisterende = brevStore[request.unikReferanse]
                    if (eksisterende != null) {
                        call.respond(status = HttpStatusCode.Conflict, BestillBrevResponse(eksisterende.referanse))
                        return@post
                    }

                    val brevbestillingReferanse = UUID.randomUUID()
                    val status = if (request.ferdigstillAutomatisk) {
                        Status.FERDIGSTILT
                    } else {
                        Status.UNDER_ARBEID
                    }
                    synchronized(mutex) {
                        brevStore.put(
                            request.unikReferanse, brevbestilling(
                                brevbestillingReferanse = brevbestillingReferanse,
                                status = status,
                                brevtype = request.brevtype,
                                brev = Brev(
                                    kanSendesAutomatisk = false,
                                    journalpostTittel = "En tittel",
                                    overskrift = "Overskrift H1",
                                    kanOverstyreBrevtittel = false,
                                    tekstbolker = listOf(
                                        Tekstbolk(
                                            id = UUID.randomUUID(),
                                            overskrift = "Overskrift H2",
                                            innhold = listOf(
                                                Innhold(
                                                    id = UUID.randomUUID(),
                                                    overskrift = "Overskrift H3",
                                                    blokker = emptyList(),
                                                    kanRedigeres = true,
                                                    erFullstendig = false
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    }
                    call.respond(status = HttpStatusCode.Created, BestillBrevResponse(brevbestillingReferanse))
                }
                post("/v3/bestill") {
                    val request = call.receive<BestillBrevV2Request>()
                    val eksisterende = brevStore[request.unikReferanse]
                    if (eksisterende != null) {
                        call.respond(status = HttpStatusCode.Conflict, BestillBrevResponse(eksisterende.referanse))
                        return@post
                    }

                    val brevbestillingReferanse = UUID.randomUUID()
                    val status = if (request.ferdigstillAutomatisk) {
                        Status.FERDIGSTILT
                    } else {
                        Status.UNDER_ARBEID
                    }
                    val brevmal = """
                        {
                          "_id": "id1",
                          "overskrift": "Mal 1",
                          "journalposttittel": "Mal 1",
                          "kanSendesAutomatisk": false,
                          "_type": "mal",
                          "delmaler": [
                            {
                              "obligatorisk": false,
                              "_type": "delmalRef",
                              "delmal": {
                                "_type": "delmal",
                                "overskrift": "Delmal",
                                "beskrivelse": "Delmal",
                                "teksteditor": [
                                  {
                                    "style": "normal",
                                    "_key": "key1",
                                    "markDefs": [],
                                    "children": [
                                      {
                                        "_type": "span",
                                        "marks": [],
                                        "text": "Delmal.",
                                        "_key": "key2"
                                      }
                                    ],
                                    "_type": "block"
                                  }
                                ],
                                "_id": "id2",
                                "paragraf": "11-3"
                              },
                              "_key": "key3"
                            }
                          ]
                        }
                    """.trimIndent()
                    synchronized(mutex) {
                        brevStore.put(
                            request.unikReferanse, brevbestilling(
                                brevbestillingReferanse = brevbestillingReferanse,
                                status = status,
                                brevtype = request.brevtype,
                                brevmal = brevmal,
                                brevdata = BrevdataDto(
                                    delmaler = emptyList(),
                                    valg = emptyList(),
                                    betingetTekst = emptyList(),
                                    fritekster = emptyList(),
                                )
                            )
                        )
                    }
                    call.respond(status = HttpStatusCode.Created, BestillBrevResponse(brevbestillingReferanse))
                }
                route("/bestilling/{referanse}") {
                    get {
                        val ref = UUID.fromString(call.pathParameters["referanse"])!!

                        call.respond(
                            synchronized(mutex) {
                                brevStore.values.find { it.referanse == ref }!!
                            }
                        )
                    }
                    post("/forhandsvis") {
                        call.respond(
                            this.javaClass.classLoader.getResourceAsStream("sample.pdf")?.readAllBytes() ?: ByteArray(0)
                        )
                    }
                    put("/oppdater") {
                        val ref = UUID.fromString(call.pathParameters["referanse"])!!
                        val brev = call.receive<Brev>()

                        val key = brevStore.entries.find { it.value.referanse == ref }?.key ?: return@put call.respond(
                            HttpStatusCode.BadRequest
                        )
                        val value = brevStore.getValue(key)
                        if (value.status != Status.UNDER_ARBEID) {
                            call.respond(HttpStatusCode.BadRequest)
                        } else {
                            synchronized(mutex) {
                                brevStore.replace(key, value.copy(brev = brev))
                            }
                            call.respond(HttpStatusCode.NoContent, Unit)
                        }
                    }
                    put("/v3/oppdater") {
                        val ref = UUID.fromString(call.pathParameters["referanse"])!!
                        val brevdata = call.receive<BrevdataDto>()

                        val key = brevStore.entries.find { it.value.referanse == ref }?.key ?: return@put call.respond(
                            HttpStatusCode.BadRequest
                        )
                        val value = brevStore.getValue(key)
                        if (value.status != Status.UNDER_ARBEID) {
                            call.respond(HttpStatusCode.BadRequest)
                        } else {
                            synchronized(mutex) {
                                brevStore.replace(key, value.copy(brevdata = brevdata))
                            }
                            call.respond(HttpStatusCode.NoContent, Unit)
                        }
                    }
                }
                post("/avbryt") {
                    val ref = call.receive<AvbrytBrevbestillingRequest>().referanse
                    val key = brevStore.entries.find { it.value.referanse == ref }?.key ?: return@post call.respond(
                        HttpStatusCode.BadRequest
                    )
                    synchronized(mutex) {
                        val value = brevStore.getValue(key)
                        brevStore.replace(key, value.copy(status = Status.AVBRUTT))
                    }
                    call.respond(HttpStatusCode.Accepted, Unit)
                }
                post("/ferdigstill") {
                    val ref = call.receive<FerdigstillBrevRequest>().referanse
                    val key = brevStore.entries.find { it.value.referanse == ref }?.key ?: return@post call.respond(
                        HttpStatusCode.BadRequest
                    )
                    synchronized(mutex) {
                        val value = brevStore.getValue(key)
                        brevStore.replace(key, value.copy(status = Status.FERDIGSTILT))
                    }
                    call.respond(HttpStatusCode.Accepted, Unit)
                }
                post("/forhandsvis-signaturer") {
                    val request = call.receive<HentSignaturerRequest>()

                    val signaturer = request.signaturGrunnlag.map {
                        Signatur(
                            navn = "Navn ${it.navIdent}",
                            enhet = "Nav Enheten"
                        )
                    }
                    val response = HentSignaturerResponse(signaturer)
                    call.respond(response)
                }
                put("/oppdater-brevmal") {
                    call.respond(HttpStatusCode.NoContent, Unit)
                }
                post("/{referanse}/kan-distribuere-brev") {
                    val req = call.receive<KanDistribuereBrevRequest>()
                    val mottakerDistStatus = req.mottakerIdentListe.map { MottakerDistStatus(it, true) }
                    call.respond(HttpStatusCode.Accepted, KanDistribuereBrevReponse(mottakerDistStatus))
                }
            }
        }
    }

    private fun brevbestilling(
        brevbestillingReferanse: UUID,
        status: Status,
        brevtype: Brevtype,
        brev: Brev? = null,
        brevmal: String? = null,
        brevdata: BrevdataDto? = null,
    ) = BrevbestillingResponse(
        referanse = brevbestillingReferanse,
        brev = brev,
        brevmal = brevmal,
        brevdata = brevdata,
        opprettet = LocalDateTime.now(),
        oppdatert = LocalDateTime.now(),
        behandlingReferanse = UUID.randomUUID(),
        brevtype = brevtype,
        språk = Språk.NB,
        status = status,
    )
}
