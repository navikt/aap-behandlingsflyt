package no.nav.aap.behandlingsflyt.integrasjon.pdl

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.io.InputStream
import java.net.URI
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class PdlBarnGatewayTest {

    private lateinit var gateway: PdlBarnGateway
    private val mockPerson: Person = Person(
        id = PersonId(0L),
        identifikator = UUID.randomUUID(),
        identer = listOf(Ident("12345678901"))
    )

    @BeforeAll
    fun setupMocks() {
        mockkStatic("no.nav.aap.komponenter.config.ConfigUtilsKt")
        every { requiredConfigForKey(any()) } answers { "mock-${firstArg<String>()}" }

        mockkObject(PdlGateway)

        val mockClient = mockk<RestClient<InputStream>>()
        every { PdlGateway getProperty "client" } returns mockClient
        every { PdlGateway getProperty "url" } returns URI.create("http://mock-url")
    }

    @AfterAll
    fun cleanupMocks() {
        unmockkAll()
    }

    @BeforeEach
    fun setup() {
        gateway = PdlBarnGateway()
    }

    @Test
    fun `hentBarn skal returnere barn med ident fra register`() {
        val barnIdent = "12345678902"
        val pdlResponse = lagPdlRelasjonResponse(listOf(lagBarnRelasjonMedIdent(barnIdent)))
        val pdlBolkResponse = lagPdlBolkResponse(listOf(Triple(barnIdent, "2015-01-01", null)))

        val mockClient = PdlGateway.client
        every {
            mockClient.post<Any, PdlRelasjonDataResponse>(
                any(),
                any(),
                any()
            )
        } returnsMany listOf(pdlResponse, pdlBolkResponse)

        val resultat = gateway.hentBarn(mockPerson, emptyList(), emptyList())

        assertEquals(1, resultat.registerBarn.size)
        val barn = resultat.registerBarn.first()
        assertTrue(barn.ident is BarnIdentifikator.BarnIdent)
        assertNotNull(barn.fødselsdato)
    }

    @Test
    fun `hentBarn skal returnere barn uten ident når de har navn og fødselsdato`() {
        val pdlResponse = lagPdlRelasjonResponse(
            listOf(lagBarnRelasjonUtenIdent("Ella", "Hansen", "2015-01-01"))
        )

        val mockClient = PdlGateway.client
        every {
            mockClient.post<Any, PdlRelasjonDataResponse>(
                any(),
                any(),
                any()
            )
        } returns pdlResponse

        val resultat = gateway.hentBarn(mockPerson, emptyList(), emptyList())

        assertThat(resultat.registerBarn.size).isEqualTo(1)

        val barn = resultat.registerBarn.first()
        assertTrue(barn.ident is BarnIdentifikator.NavnOgFødselsdato)
        assertThat(barn.ident).isInstanceOf(BarnIdentifikator.NavnOgFødselsdato::class.java)
        assertThat(barn.navn).isEqualTo("Ella Hansen")
    }

    @Test
    fun `hentBarn skal kaste feil når barn mangler både ident og navn-fødselsdato kombinasjon`() {
        val pdlResponse = lagPdlRelasjonResponse(
            listOf(lagBarnRelasjonUtenIdentOgData())
        )

        val mockClient = PdlGateway.client
        every {
            mockClient.post<Any, PdlRelasjonDataResponse>(
                any(),
                any(),
                any()
            )
        } returns pdlResponse

        assertThrows<IllegalStateException> {
            gateway.hentBarn(mockPerson, emptyList(), emptyList())
        }
    }

    @Test
    fun `hentBarn skal returnere oppgitte barn separat`() {
        val oppgittIdent = Ident("12345678903")
        val pdlResponse = lagPdlRelasjonResponse(emptyList())
        val pdlBolkResponse = lagPdlBolkResponse(listOf(Triple(oppgittIdent.identifikator, "2016-01-01", null)))

        val mockClient = PdlGateway.client
        every {
            mockClient.post<Any, PdlRelasjonDataResponse>(
                any(),
                any(),
                any()
            )
        } returnsMany listOf(pdlResponse, pdlBolkResponse)

        val resultat = gateway.hentBarn(mockPerson, listOf(oppgittIdent), emptyList())

        assertThat(resultat.oppgitteBarnFraPDL.size).isEqualTo(1)
        assertThat(resultat.registerBarn.size).isEqualTo(0)
    }

    @Test
    fun `hentBarn skal håndtere tom liste med identer`() {
        val pdlResponse = lagPdlRelasjonResponse(emptyList())

        val mockClient = PdlGateway.client
        every {
            mockClient.post<Any, PdlRelasjonDataResponse>(
                any(),
                any(),
                any()
            )
        } returns pdlResponse

        val resultat = gateway.hentBarn(mockPerson, emptyList(), emptyList())

        assertTrue(resultat.registerBarn.isEmpty())
        assertTrue(resultat.oppgitteBarnFraPDL.isEmpty())
    }

    @Test
    fun `hentBarn skal inkludere dødsdato når den finnes`() {
        val barnIdent = "12345678902"
        val pdlResponse = lagPdlRelasjonResponse(listOf(lagBarnRelasjonMedIdent(barnIdent)))
        val pdlBolkResponse = lagPdlBolkResponse(
            listOf(Triple(barnIdent, "2015-01-01", "2020-01-01"))
        )

        val mockClient = PdlGateway.client
        every {
            mockClient.post<Any, PdlRelasjonDataResponse>(
                any(),
                any(),
                any()
            )
        } returnsMany listOf(pdlResponse, pdlBolkResponse)

        val resultat = gateway.hentBarn(mockPerson, emptyList(), emptyList())

        val barn = resultat.registerBarn.first()
        assertNotNull(barn.dødsdato)
    }

    @Test
    fun `hentBarn skal mappe dødsdato fra PDL bolk respons`() {
        val barnIdent = "12345678902"
        val pdlResponse = lagPdlRelasjonResponse(listOf(lagBarnRelasjonMedIdent(barnIdent)))
        val pdlBolkResponse = lagPdlBolkResponse(
            listOf(Triple(barnIdent, "2015-01-01", "2020-06-15"))
        )

        val mockClient = PdlGateway.client
        every {
            mockClient.post<Any, PdlRelasjonDataResponse>(
                any(),
                any(),
                any()
            )
        } returnsMany listOf(pdlResponse, pdlBolkResponse)

        val resultat = gateway.hentBarn(mockPerson, emptyList(), emptyList())

        assertEquals(1, resultat.registerBarn.size)
        val barn = resultat.registerBarn.first()
        assertNotNull(barn.dødsdato)
        assertEquals("2020-06-15", barn.dødsdato!!.toFormatedString())
    }

    @Test
    fun `hentBarn skal separere barn i riktige kategorier basert på kilde`() {
        val registerIdent = "12345678902"
        val oppgittIdent = Ident("12345678903")
        val saksbehandlerIdent = Ident("12345678904")

        val pdlRelasjonResponse = lagPdlRelasjonResponse(listOf(lagBarnRelasjonMedIdent(registerIdent)))
        val pdlBolkResponse = lagPdlBolkResponse(
            listOf(
                Triple(registerIdent, "2015-01-01", null),
                Triple(oppgittIdent.identifikator, "2016-01-01", null),
                Triple(saksbehandlerIdent.identifikator, "2017-01-01", null)
            )
        )

        val mockClient = PdlGateway.client
        every {
            mockClient.post<Any, PdlRelasjonDataResponse>(
                any(),
                any(),
                any()
            )
        } returnsMany listOf(pdlRelasjonResponse, pdlBolkResponse)

        val resultat = gateway.hentBarn(mockPerson, listOf(oppgittIdent), listOf(saksbehandlerIdent))

        assertEquals(1, resultat.registerBarn.size)
        assertEquals(1, resultat.oppgitteBarnFraPDL.size)
        assertEquals(1, resultat.saksbehandlerOppgitteBarnPDL.size)

        assertEquals(
            registerIdent,
            (resultat.registerBarn.first().ident as BarnIdentifikator.BarnIdent).ident.identifikator
        )
        assertEquals(
            oppgittIdent.identifikator,
            (resultat.oppgitteBarnFraPDL.first().ident as BarnIdentifikator.BarnIdent).ident.identifikator
        )
        assertEquals(
            saksbehandlerIdent.identifikator,
            (resultat.saksbehandlerOppgitteBarnPDL.first().ident as BarnIdentifikator.BarnIdent).ident.identifikator
        )
    }

    @Test
    fun `hentBarn skal håndtere overlappende identer mellom kilder`() {
        val felles = "12345678902"
        val pdlRelasjonResponse = lagPdlRelasjonResponse(listOf(lagBarnRelasjonMedIdent(felles)))
        val pdlBolkResponse = lagPdlBolkResponse(listOf(Triple(felles, "2015-01-01", null)))

        val mockClient = PdlGateway.client
        every {
            mockClient.post<Any, PdlRelasjonDataResponse>(
                any(),
                any(),
                any()
            )
        } returnsMany listOf(pdlRelasjonResponse, pdlBolkResponse)

        val resultat = gateway.hentBarn(mockPerson, listOf(Ident(felles)), listOf(Ident(felles)))

        assertEquals(1, resultat.registerBarn.size)
        assertEquals(1, resultat.oppgitteBarnFraPDL.size)
        assertEquals(1, resultat.saksbehandlerOppgitteBarnPDL.size)
    }

    @Test
    fun `hentBarn skal fjerne duplikater når samme ident forekommer flere ganger`() {
        val ident = "12345678902"
        val pdlRelasjonResponse = lagPdlRelasjonResponse(emptyList())
        val pdlBolkResponse = lagPdlBolkResponse(listOf(Triple(ident, "2015-01-01", null)))

        val mockClient = PdlGateway.client
        every {
            mockClient.post<Any, PdlRelasjonDataResponse>(
                any(),
                any(),
                any()
            )
        } returnsMany listOf(pdlRelasjonResponse, pdlBolkResponse)

        val resultat = gateway.hentBarn(
            mockPerson,
            listOf(Ident(ident), Ident(ident)),
            listOf(Ident(ident))
        )

        val alleBarn = resultat.registerBarn + resultat.oppgitteBarnFraPDL + resultat.saksbehandlerOppgitteBarnPDL
        assertEquals(3, alleBarn.size)
    }

    // Hjelpemetoder for å lage testdata
    private fun lagPdlRelasjonResponse(relasjoner: List<ForelderBarnRelasjon>): PdlRelasjonDataResponse {
        return PdlRelasjonDataResponse(
            data = PdlRelasjonData(hentPerson = PdlPersoninfo(forelderBarnRelasjon = relasjoner)),
            errors = null,
            extensions = null
        )
    }

    private fun lagBarnRelasjonMedIdent(ident: String): ForelderBarnRelasjon {
        return ForelderBarnRelasjon(
            relatertPersonsIdent = ident,
            relatertPersonsRolle = ForelderBarnRelasjonRolle.BARN,
            relatertPersonUtenFolkeregisteridentifikator = null,
        )
    }

    private fun lagBarnRelasjonUtenIdent(
        fornavn: String,
        etternavn: String,
        fødselsdato: String
    ): ForelderBarnRelasjon {
        return ForelderBarnRelasjon(
            relatertPersonsRolle = ForelderBarnRelasjonRolle.BARN,
            relatertPersonsIdent = null,
            relatertPersonUtenFolkeregisteridentifikator = RelatertBiPerson(
                foedselsdato = LocalDate.parse(fødselsdato),
                statsborgerskap = null,
                navn = PdlNavn(fornavn = fornavn, mellomnavn = null, etternavn = etternavn)
            )
        )
    }

    private fun lagBarnRelasjonUtenIdentOgData(): ForelderBarnRelasjon {
        return ForelderBarnRelasjon(
            relatertPersonsIdent = null,
            relatertPersonsRolle = ForelderBarnRelasjonRolle.BARN,
            relatertPersonUtenFolkeregisteridentifikator = RelatertBiPerson(
                foedselsdato = null,
                navn = null,
                statsborgerskap = null
            )
        )
    }

    private fun lagPdlBolkResponse(barnData: List<Triple<String, String, String?>>): PdlRelasjonDataResponse {
        val personBolkList = barnData.map { (ident, fødselsdato, dødsdato) ->
            HentPersonBolkResult(
                ident = ident,
                person = PdlPersoninfo(
                    forelderBarnRelasjon = null,
                    foedselsdato = listOf(
                        PdlFoedsel(
                            foedselsdato = fødselsdato,
                            foedselAar = null,
                        )
                    ),
                    doedsfall = dødsdato?.let { PDLDødsfall(doedsdato = it) }?.let(::setOf),
                    statsborgerskap = null,
                    folkeregisterpersonstatus = null,
                    bostedsadresse = null,
                    oppholdsadresse = null,
                    kontaktadresse = null,
                    navn = listOf(PdlNavn(fornavn = "Janne", mellomnavn = null, etternavn = "Larsen"))
                )
            )
        }

        return PdlRelasjonDataResponse(
            data = PdlRelasjonData(
                hentPerson = null,
                hentPersonBolk = personBolkList
            ),
            errors = null,
            extensions = null,
        )
    }
}
