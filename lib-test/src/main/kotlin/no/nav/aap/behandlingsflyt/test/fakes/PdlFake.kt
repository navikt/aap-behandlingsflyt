package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.integrasjon.ident.IDENT_QUERY
import no.nav.aap.behandlingsflyt.integrasjon.ident.PERSONINFO_BOLK_QUERY
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.pdl.BARN_RELASJON_QUERY
import no.nav.aap.behandlingsflyt.integrasjon.pdl.ForelderBarnRelasjon
import no.nav.aap.behandlingsflyt.integrasjon.pdl.ForelderBarnRelasjonRolle
import no.nav.aap.behandlingsflyt.integrasjon.pdl.HentPerson
import no.nav.aap.behandlingsflyt.integrasjon.pdl.HentPersonBolkResult
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PDLDødsfall
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PERSON_BOLK_QUERY
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PERSON_QUERY
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PERSON_QUERY_HISTORIKK
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlFoedsel
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlFolkeregisterPersonStatus
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlGruppe
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlIdent
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlIdenter
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlIdenterData
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlIdenterDataResponse
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlNavn
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlNavnData
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlNavnDataBolk
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlPersonBolk
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlPersonNavnDataResponse
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlPersoninfo
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlPersoninfoData
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlPersoninfoDataResponse
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlRelasjonData
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlRelasjonDataResponse
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlRequest
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PdlStatsborgerskap
import no.nav.aap.behandlingsflyt.integrasjon.pdl.PersonStatus
import no.nav.aap.behandlingsflyt.test.TestPersonService
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import java.time.LocalDate

class PdlFake(private val fakePersoner: () -> TestPersonService) : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("PDL")
        routing {
            post {
                val req = call.receive<PdlRequest>()

                when (req.query) {
                    IDENT_QUERY -> call.respond(identer(req))
                    PERSON_QUERY -> call.respond(personopplysninger(req))
                    PERSON_QUERY_HISTORIKK -> call.respond(personopplysningerHistorikk(req))
                    PdlPersoninfoGateway.PERSONINFO_QUERY -> call.respond(navn(req))
                    BARN_RELASJON_QUERY -> call.respond(barnRelasjoner(req))
                    PERSON_BOLK_QUERY -> call.respond(barn(req))
                    PERSONINFO_BOLK_QUERY -> call.respond(personinfoBolk(req))
                    else -> call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }

    private fun barn(req: PdlRequest): PdlRelasjonDataResponse {
        val forespurtIdenter = req.variables.identer.orEmpty()

        val barnIdenter = forespurtIdenter.mapNotNull { mapIdentBolk(it) }.toList()

        return PdlRelasjonDataResponse(
            errors = null,
            extensions = null,
            data = PdlRelasjonData(
                hentPersonBolk = barnIdenter
            )
        )
    }

    private fun personinfoBolk(req: PdlRequest): PdlPersonNavnDataResponse {
        val forespurtIdenter = req.variables.identer.orEmpty()

        val navnBolk = forespurtIdenter.mapNotNull { ident ->
            val person = fakePersoner().hentPerson(ident) ?: return@mapNotNull null
            PdlNavnDataBolk(
                ident = person.identer.first().identifikator,
                person = PdlPersonBolk(
                    navn = listOf(
                        PdlNavn(
                            fornavn = person.navn.fornavn,
                            mellomnavn = null,
                            etternavn = person.navn.etternavn
                        )
                    )
                )
            )
        }

        return PdlPersonNavnDataResponse(
            errors = null,
            extensions = null,
            data = HentPerson(
                hentPersonBolk = navnBolk
            )
        )
    }

    private fun mapIdentBolk(it: String): HentPersonBolkResult? {
        val person = fakePersoner().hentPerson(it) ?: return null
        return HentPersonBolkResult(
            ident = person.identer.first().identifikator,
            person = PdlPersoninfo(
                navn = listOf(
                    PdlNavn(
                        fornavn = person.navn.fornavn,
                        mellomnavn = null,
                        etternavn = person.navn.etternavn
                    )
                ),
                foedselsdato = listOf(
                    PdlFoedsel(
                        person.fødselsdato.toFormattedString(),
                        "" + person.fødselsdato.toLocalDate().year
                    )
                ),
                doedsfall = mapDødsfall(person),
                statsborgerskap = setOf(PdlStatsborgerskap("NOR", LocalDate.now().minusYears(5), LocalDate.now())),
                folkeregisterpersonstatus = setOf(PdlFolkeregisterPersonStatus(PersonStatus.bosatt, null))
            )
        )
    }

    private fun mapDødsfall(person: TestPerson): Set<PDLDødsfall>? {
        if (person.dødsdato == null) {
            return null
        }
        return setOf(PDLDødsfall(person.dødsdato.toFormatedString()))
    }

    private fun barnRelasjoner(req: PdlRequest): PdlRelasjonDataResponse {
        val testPerson = hentEllerGenererTestPerson(fakePersoner(), req.variables.ident ?: "")
        return PdlRelasjonDataResponse(
            errors = null,
            extensions = null,
            data = PdlRelasjonData(
                hentPerson = PdlPersoninfo(
                    forelderBarnRelasjon = testPerson.barn
                        .map {
                            ForelderBarnRelasjon(
                                it.identer.first().identifikator,
                                relatertPersonsRolle = ForelderBarnRelasjonRolle.BARN,
                            )
                        }
                        .toList(),
                    statsborgerskap = setOf(PdlStatsborgerskap("NOR", LocalDate.now().minusYears(5), LocalDate.now())),
                    folkeregisterpersonstatus = setOf(PdlFolkeregisterPersonStatus(PersonStatus.bosatt, null))
                )
            )
        )
    }

    private fun identer(req: PdlRequest): PdlIdenterDataResponse {
        val person = hentEllerGenererTestPerson(fakePersoner(), req.variables.ident ?: "")

        return PdlIdenterDataResponse(
            errors = null,
            extensions = null,
            data = PdlIdenterData(
                hentIdenter = PdlIdenter(
                    identer = mapIdent(person)
                )
            ),
        )
    }

    private fun mapIdent(person: TestPerson?): List<PdlIdent> {
        if (person == null) {
            return emptyList()
        }
        return listOf(
            PdlIdent(
                person.identer.first().identifikator,
                false,
                PdlGruppe.FOLKEREGISTERIDENT
            )
        )
    }

    private fun personopplysninger(req: PdlRequest): PdlPersoninfoDataResponse {
        val testPerson = hentEllerGenererTestPerson(fakePersoner(), req.variables.ident ?: "")
        return PdlPersoninfoDataResponse(
            errors = null,
            extensions = null,
            data = PdlPersoninfoData(
                hentPerson = PdlPersoninfo(
                    foedselsdato = listOf(
                        PdlFoedsel(
                            testPerson.fødselsdato.toFormattedString(),
                            "" + testPerson.fødselsdato.toLocalDate().year
                        )
                    ),
                    statsborgerskap = setOf(PdlStatsborgerskap("NOR", LocalDate.now(), LocalDate.now())),
                    folkeregisterpersonstatus = setOf(PdlFolkeregisterPersonStatus(PersonStatus.bosatt, null))
                )
            ),
        )
    }

    private fun personopplysningerHistorikk(req: PdlRequest): PdlPersoninfoDataResponse {
        val testPerson = hentEllerGenererTestPerson(fakePersoner(), req.variables.ident ?: "")
        return PdlPersoninfoDataResponse(
            errors = null,
            extensions = null,
            data = PdlPersoninfoData(
                hentPerson = testPerson.let { person ->
                    PdlPersoninfo(
                        foedselsdato = listOf(
                            PdlFoedsel(
                                person.fødselsdato.toFormattedString(),
                                "" + person.fødselsdato.toLocalDate().year
                            )
                        ),
                        statsborgerskap = person.statsborgerskap.toSet(),
                        folkeregisterpersonstatus = person.personStatus.toSet(),
                    )
                }
            ),
        )
    }

    private fun navn(req: PdlRequest): PdlPersonNavnDataResponse {
        val testPerson = hentEllerGenererTestPerson(fakePersoner(), req.variables.ident ?: "")
        return PdlPersonNavnDataResponse(
            errors = null,
            extensions = null,
            data = HentPerson(
                hentPerson = PdlNavnData(
                    ident = testPerson.identer.first().identifikator,
                    navn = listOf(
                        PdlNavn(
                            fornavn = testPerson.navn.fornavn,
                            mellomnavn = null,
                            etternavn = testPerson.navn.etternavn
                        )
                    )
                )
            ),
        )
    }
}
