package no.nav.aap.behandlingsflyt

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.behandling.bruddaktivitetsplikt.GrunnDTO
import no.nav.aap.behandlingsflyt.behandling.bruddaktivitetsplikt.OpprettAktivitetspliktDTO
import no.nav.aap.behandlingsflyt.behandling.bruddaktivitetsplikt.PeriodeDTO
import no.nav.aap.behandlingsflyt.behandling.grunnlag.medlemskap.MedlemskapGrunnlagDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemKode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemMedl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapDataIntern
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.flyt.VilkårDTO
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.FinnEllerOpprettSakDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksinfoDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.UtvidetSaksinfoDTO
import no.nav.aap.behandlingsflyt.test.AzureTokenGen
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.FakeServers
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.*

@Fakes
class ApiTest {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val postgres = postgreSQLContainer()
        private lateinit var port: Number

        private val dbConfig = DbConfig(
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password
        )

        private val client: RestClient<InputStream> = RestClient(
            config = ClientConfig(scope = "behandlingsflyt"),
            tokenProvider = OnBehalfOfTokenProvider,
            responseHandler = DefaultResponseHandler()
        )

        private val ccClient: RestClient<InputStream> = RestClient(
            config = ClientConfig(scope = "behandlingsflyt"),
            tokenProvider = ClientCredentialsTokenProvider,
            responseHandler = DefaultResponseHandler()
        )

        private val noTokenClient: RestClient<InputStream> = RestClient(
            config = ClientConfig(scope = "behandlingsflyt"),
            tokenProvider = NoTokenTokenProvider(),
            responseHandler = DefaultResponseHandler()
        )

        private var token: OidcToken? = null
        private fun getToken(): OidcToken {
            val client = RestClient(
                config = ClientConfig(scope = "behandlingsflyt"),
                tokenProvider = NoTokenTokenProvider(),
                responseHandler = DefaultResponseHandler()
            )
            return token ?: OidcToken(
                client.post<Unit, FakeServers.TestToken>(
                    URI.create(requiredConfigForKey("azure.openid.config.token.endpoint")),
                    PostRequest(Unit)
                )!!.access_token
            )
        }

        // Starter server
        private val server = embeddedServer(Netty, port = 0) {
            server(dbConfig = dbConfig, repositoryRegistry = postgresRepositoryRegistry)
        }

        @JvmStatic
        @BeforeAll
        fun beforeall() {
            System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
            server.start()
            port =
                runBlocking { server.engine.resolvedConnectors().first { it.type == ConnectorType.HTTP }.port }
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.stop()
            postgres.close()
        }
    }

    @Test
    fun `kalle medlemsskaps-api`() {
        val ds = initDatasource(dbConfig)

        val opprettetBehandling = ds.transaction { connection ->
            val personOgSakService = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            )
            val behandlingRepo = BehandlingRepositoryImpl(connection)

            val sak =
                personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
            val behandling = behandlingRepo.opprettBehandling(
                sak.id,
                listOf(Årsak(type = ÅrsakTilBehandling.MOTTATT_SØKNAD)),
                TypeBehandling.Førstegangsbehandling, null
            )
            val medlRepo = postgresRepositoryRegistry.provider(connection).provide<MedlemskapRepository>()
            medlRepo.lagreUnntakMedlemskap(
                behandlingId = behandling.id,
                listOf(
                    MedlemskapDataIntern(
                        unntakId = 123,
                        fraOgMed = "2017-02-13",
                        tilOgMed = "2018-02-13",
                        grunnlag = "grunnlag",
                        helsedel = true,
                        ident = "02429118789",
                        lovvalg = "lovvalg",
                        medlem = true,
                        status = "GYLD",
                        statusaarsak = null,
                        lovvalgsland = "NORGE",
                        kilde = KildesystemMedl(KildesystemKode.MEDL, "MEDL")
                    )
                )
            )
            return@transaction behandling
        }

        val medlemskapGrunnlag: MedlemskapGrunnlagDto? = client.get(
            URI.create("http://localhost:$port/")
                .resolve("api/behandling/${opprettetBehandling.referanse}/grunnlag/medlemskap"),
            GetRequest(currentToken = getToken())
        )

        assertThat(medlemskapGrunnlag).isNotNull
        assertThat(medlemskapGrunnlag?.medlemskap?.unntak).isNotEmpty
    }

    @Test
    fun `kalle beregningsgrunnlag-api`() {
        val ds = initDatasource(dbConfig)
        val referanse = ds.transaction { connection ->
            val personOgSakService = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            )
            val behandlingRepo = BehandlingRepositoryImpl(connection)
            val sak =
                personOgSakService.finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(3)))
            val behandling = behandlingRepo.opprettBehandling(
                sak.id,
                listOf(Årsak(type = ÅrsakTilBehandling.MOTTATT_SØKNAD)),
                TypeBehandling.Førstegangsbehandling, null
            )
            val beregningsgrunnlagRepository = BeregningsgrunnlagRepositoryImpl(connection)
            beregningsgrunnlagRepository.lagre(
                behandlingId = behandling.id,
                Grunnlag11_19(
                    grunnlaget = GUnit(3),
                    erGjennomsnitt = false,
                    gjennomsnittligInntektIG = GUnit(3),
                    inntekter = listOf(
                        GrunnlagInntekt(
                            år = Year.of(2023),
                            inntektIKroner = Beløp(200000),
                            grunnbeløp = Beløp(100000),
                            inntektIG = GUnit(3),
                            inntekt6GBegrenset = GUnit(3),
                            er6GBegrenset = false
                        ),
                        GrunnlagInntekt(
                            år = Year.of(2022),
                            inntektIKroner = Beløp(200000),
                            grunnbeløp = Beløp(100000),
                            inntektIG = GUnit(3),
                            inntekt6GBegrenset = GUnit(3),
                            er6GBegrenset = false
                        ),
                        GrunnlagInntekt(
                            år = Year.of(2021),
                            inntektIKroner = Beløp(200000),
                            grunnbeløp = Beløp(100000),
                            inntektIG = GUnit(3),
                            inntekt6GBegrenset = GUnit(3),
                            er6GBegrenset = false
                        )
                    )
                )
            )
            behandling.referanse
        }

        val dagensDato = LocalDate.now()

        val asJSON: JsonNode? = client.get(
            URI.create("http://localhost:$port/api/beregning/grunnlag/").resolve(referanse.toString()),
            GetRequest(currentToken = getToken()),
        ) { x, _ -> ObjectMapper().readTree(x) }

        @Language("JSON") val expectedJSON =
            """{
  "beregningstypeDTO": "STANDARD",
  "grunnlag11_19": {
    "nedsattArbeidsevneÅr": "2024",
    "inntekter": [
      {
        "år": "2021",
        "inntektIKroner": 200000.0,
        "inntektIG": 3.0,
        "justertTilMaks6G": 3.0
      },
      {
        "år": "2022",
        "inntektIKroner": 200000.0,
        "inntektIG": 3.0,
        "justertTilMaks6G": 3.0
      },
      {
        "år": "2023",
        "inntektIKroner": 200000.0,
        "inntektIG": 3.0,
        "justertTilMaks6G": 3.0
      }
    ],
    "gjennomsnittligInntektSiste3år": 3.0,
    "inntektSisteÅr": {
      "år": "2023",
      "inntektIKroner": 200000.0,
      "inntektIG": 3.0,
      "justertTilMaks6G": 3.0
    },
    "grunnlag": 3.0
  },
  "grunnlagYrkesskade": null,
  "grunnlagUføre": null,
  "grunnlagYrkesskadeUføre": null,
  "gjeldendeGrunnbeløp": {
    "grunnbeløp":130160.0,
    "dato": "$dagensDato"
  }
}"""
        Assertions.assertThat(asJSON).isEqualTo(ObjectMapper().readTree(expectedJSON))
    }

    @Test
    fun test() {
        FakePersoner.leggTil(
            TestPerson(
                identer = setOf(Ident("12345678910")),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                yrkesskade = emptyList()
            )
        )

        val responseSak: SaksinfoDTO? = ccClient.post(
            URI.create("http://localhost:$port/").resolve("api/sak/finnEllerOpprett"),
            PostRequest(
                body = FinnEllerOpprettSakDTO("12345678910", LocalDate.now()),
            )
        )

        requireNotNull(responseSak)

        noTokenClient.post<_, Unit>(
            URI.create("http://localhost:$port/").resolve("api/hendelse/send"),
            PostRequest(
                body = Innsending(
                    saksnummer = Saksnummer(responseSak.saksnummer),
                    referanse = InnsendingReferanse(JournalpostId("123456789")),
                    type = InnsendingType.SØKNAD,// Søknad(SøknadStudentDto("NEI"), "NEI", null),
                    mottattTidspunkt = LocalDateTime.now(),
                    melding = SøknadV0(
                        student = null,
                        yrkesskade = "NEI",
                        oppgitteBarn = null,
                        medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
                    )
                ),
                additionalHeaders = listOf(
                    azpAuth(Azp.Postmottak)
                ),
            )
        )


        val utvidetSak = kallInntilKlar { hentUtivdedSaksInfo(responseSak) }

        requireNotNull(utvidetSak)

        data class EndringDTO(
            val status: Status,
            val tidsstempel: LocalDateTime = LocalDateTime.now(),
            val begrunnelse: String,
            val endretAv: String
        )

        data class AvklaringsbehovDTO(
            val definisjon: Any,
            val status: Status,
            val endringer: List<EndringDTO>
        )

        data class DetaljertBehandlingDTO(
            val referanse: UUID,
            val type: String,
            val status: no.nav.aap.behandlingsflyt.kontrakt.behandling.Status,
            val opprettet: LocalDateTime,
            val avklaringsbehov: List<AvklaringsbehovDTO>,
            val vilkår: List<VilkårDTO>,
            val aktivtSteg: StegType,
            val versjon: Long,
            val vedtaksdato: LocalDate?,
            val virkningstidspunkt: LocalDate?
        )

        val behandling = kallInntilKlar {
            client.get<DetaljertBehandlingDTO>(
                URI.create("http://localhost:$port/")
                    .resolve("api/behandling/")
                    .resolve(utvidetSak.behandlinger.first().referanse.toString()),
                GetRequest(currentToken = getToken())
            )
        }

        log.info("Behandling $behandling")
        assertThat(behandling?.type).isEqualTo("Førstegangsbehandling")
        assertThat(behandling?.status).isEqualTo(no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.UTREDES)
        assertThat(behandling?.virkningstidspunkt).isNull()
        assertThat(behandling?.vedtaksdato).isNull()
        assertThat(behandling?.aktivtSteg).isEqualTo(StegType.AVKLAR_SYKDOM)

    }

    @Test
    fun `registrere aktivetsplikt, får opprettet ny behandling`() {
        FakePersoner.leggTil(
            TestPerson(
                identer = setOf(Ident("12345678910")),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(20)),
                yrkesskade = emptyList()
            )
        )

        val responseSak: SaksinfoDTO? = ccClient.post(
            URI.create("http://localhost:$port/").resolve("api/sak/finnEllerOpprett"),
            PostRequest(
                body = FinnEllerOpprettSakDTO("12345678910", LocalDate.now().minusDays(30)),
                currentToken = getToken()
            )
        )

        requireNotNull(responseSak)

        val saksnummer = responseSak.saksnummer

        client.post<Any, Map<String, String>>(
            URI.create("http://localhost:$port/").resolve("api/sak/$saksnummer/aktivitetsplikt/opprett"),
            PostRequest(
                body = OpprettAktivitetspliktDTO(
                    brudd = BruddType.IKKE_AKTIVT_BIDRAG,
                    paragraf = Brudd.Paragraf.PARAGRAF_11_7,
                    begrunnelse = "heya",
                    grunn = GrunnDTO.INGEN_GYLDIG_GRUNN,
                    perioder = listOf(
                        PeriodeDTO(
                            fom = LocalDate.now().plusDays(10),
                            tom = LocalDate.now().plusDays(20)
                        )
                    )
                ),
                currentToken = getToken()
            )
        )

        val ds = initDatasource(dbConfig)

        val behandlinger = kallInntilKlar(maxTries = 10, delayMs = 500) {
            val behandlinger = ds.transaction { connection ->
                val sak = SakRepositoryImpl(connection).hent(Saksnummer(saksnummer))
                BehandlingRepositoryImpl(connection).hentAlleFor(sak.id)
            }
            behandlinger
        }

        assertThat(behandlinger).hasSize(1)
        assertThat(behandlinger!!.first().årsaker().map { it.type }).contains(
            ÅrsakTilBehandling.MOTTATT_AKTIVITETSMELDING
        )

    }

    private fun <E> kallInntilKlar(maxTries: Int = 10, delayMs: Long = 100L, block: () -> E): E? {
        return runBlocking {
            suspend {
                var utvidedSak: E? = null
                var tries = 0
                while (tries < maxTries) {
                    try {
                        utvidedSak = block()
                    } catch (e: Exception) {
                        log.info("Exception: $e")
                    } finally {
                        delay(delayMs)
                        tries++
                    }
                }
                utvidedSak
            }.invoke()
        }
    }

    private fun hentUtivdedSaksInfo(
        responseSak: SaksinfoDTO,
    ): UtvidetSaksinfoDTO? {
        val utvidetSak3: UtvidetSaksinfoDTO? = client.get(
            URI.create("http://localhost:$port/").resolve("api/sak/").resolve(responseSak.saksnummer),
            GetRequest(currentToken = getToken())
        )
        if (utvidetSak3?.behandlinger?.isNotEmpty() == true) {
            return utvidetSak3
        }
        return null
    }

    private fun azpAuth(azp: Azp) = Header(
        "Authorization",
        "Bearer ${
            AzureTokenGen("behandlingsflyt", "behandlingsflyt").generate(
                true,
                azp.uuid.toString()
            )
        }"
    )
}


object FakePdlGateway : IdentGateway {
    override fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
        return listOf(ident)
    }
}