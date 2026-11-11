# Test skeletons

Copy, rename, and keep the house style (JUnit 5, AssertJ, backtick names in Norwegian). Imports: fakes and helpers
come from `no.nav.aap.behandlingsflyt.test.*`, `…test.inmemoryrepo.*` and `…help.*` (lib-test).

## Unit test of a step (fakes, no mocks)

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

Use the primary constructor when you need to swap one collaborator, e.g. `FakeTidligereVurderinger().apply {
avslagEllerIngenBehandlingsgrunnlag = true }`. Gateways: `minimalGatewayProvider { register<FakeOppgavestyringGateway>() }`.
Many steps expose smaller functions too (`utledBehov(kontekst)` in `SykestipendStegTest`); test those directly.

## Unit test of a løser

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

## Repository integration test (Postgres via Testcontainers)

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

Tests in the class share the database: never assume empty tables or count rows globally.

## Gateway integration test (HTTP fake server)

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

The fake server lives in `lib-test/.../test/fakes/` and is wired in `FakeServers` (see fakes.md).

## API route test (Ktor test host, in-memory, no Docker)

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

## Flyt-test

See [flyttester.md](flyttester.md). Extend an existing `*FlytTest` class when the scenario fits it; a new class pays the
database and fake start-up again.
