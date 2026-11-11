# Testmaler

Kopier og gi nytt navn, og følg kodestilen (JUnit 5, AssertJ, testnavn på norsk i backticks). Importer: fakes og hjelpere
kommer fra `no.nav.aap.behandlingsflyt.test.*`, `…test.inmemoryrepo.*` og `…help.*` (lib-test).

## Enhetstest av et steg (fakes, ingen mocks)

```kotlin
class NyttStegTest {
    @Test
    fun `krever vurdering når søker har …`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val kontekst = flytKontekstMedPerioder {
            this.behandling = behandling
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING
            rettighetsperiode = Periode(1 januar 2025, Tid.MAKS)
        }
        InMemoryNyVurderingRepository.lagre(behandling.id, /* … */)

        // the same secondary constructor production uses, fed with fakes
        val steg = NyttSteg(inMemoryRepositoryProvider, minimalGatewayProvider())
        val resultat = steg.utfør(kontekst)

        assertThat(resultat).isEqualTo(Fullført)
        assertThat(InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id).måLøses().map { it.definisjon })
            .containsExactly(Definisjon.NY_VURDERING)
    }
}
```

Bruk primærkonstruktøren når du må bytte ut én samarbeidspartner, for eksempel `FakeTidligereVurderinger().apply {
avslagEllerIngenBehandlingsgrunnlag = true }`. Gateways: `minimalGatewayProvider { register<FakeOppgavestyringGateway>() }`.
Mange steg har også mindre funksjoner (`utledBehov(kontekst)` i `SykestipendStegTest`), som du kan teste direkte.

## Enhetstest av en løser

```kotlin
class NyVurderingLøserTest {
    private val løser = NyVurderingLøser(InMemoryNyVurderingRepository)

    @Test
    fun `lagrer vurderingen på behandlingen`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        løser.løs(avklaringsbehovKontekst { this.behandling = behandling }, NyVurderingLøsning(/* … */))

        assertThat(InMemoryNyVurderingRepository.hentHvisEksisterer(behandling.id)).isNotNull
    }
}
```

## Repository-integrasjonstest (Postgres via Testcontainers)

```kotlin
internal class NyVurderingRepositoryImplTest {
    companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() { dataSource = TestDataSource() }   // fresh database cloned from the migrated template

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    @Test
    fun `lagrer, henter og kopierer til revurdering`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)                                   // new person + sak every test
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repo = NyVurderingRepositoryImpl(connection)

            repo.lagre(behandling.id, vurdering)
            assertThat(repo.hentHvisEksisterer(behandling.id)?.vurdering).isEqualTo(vurdering)

            val revurdering = BehandlingRepositoryImpl(connection).opprettBehandling(
                sak.id, TypeBehandling.Revurdering, behandling.id,
                VurderingsbehovOgÅrsak(emptyList(), ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE),
            )
            repo.kopier(behandling.id, revurdering.id)
            assertThat(repo.hentHvisEksisterer(revurdering.id)?.vurdering).isEqualTo(vurdering)

            repo.slett(behandling.id)                                   // must be idempotent
            repo.slett(behandling.id)
        }
    }
}
```

Testene i klassen deler database: Ikke anta at tabellene er tomme, og ikke tell rader globalt.

## Gateway-integrasjonstest (HTTP-fake-server)

```kotlin
@Fakes
class NyGatewayImplTest {
    @Test
    fun `henter perioder for personen`() {
        val person = FakePersoner.leggTil(TestPerson(/* data the fake answers with */))

        val respons = NyGatewayImpl().hentPerioder(person.aktivIdent().identifikator, LocalDate.now().minusYears(1), LocalDate.now())

        assertThat(respons).hasSize(1)
    }
}
```

Fake-serveren ligger i `lib-test/.../test/fakes/` og kobles til i `FakeServers` (se fakes.md).

## API-rutetest (Ktor test host, in-memory, uten Docker)

```kotlin
@Fakes
class NyVurderingApiTest : BaseApiTest() {
    @Test
    fun `henter grunnlag`() {
        val sak = opprettInMemorySak(LocalDate.parse("2025-01-01"))
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)
        InMemoryNyVurderingRepository.lagre(behandling.id, /* … */)

        testApplication {
            installApplication { nyVurderingApi(MockDataSource(), inMemoryRepositoryRegistry, minimalGatewayProvider()) }
            val respons = createClient().get("/api/behandling/${behandling.referanse.referanse}/grunnlag/ny-vurdering") {
                header("Authorization", "Bearer ${getToken().token()}")
            }
            assertThat(respons.status).isEqualTo(HttpStatusCode.OK)
        }
    }
}
```

## Flyttest

Se [flyttester.md](flyttester.md). Utvid en eksisterende `*FlytTest`-klasse når scenarioet passer. En ny klasse må starte
database og fakes på nytt.
