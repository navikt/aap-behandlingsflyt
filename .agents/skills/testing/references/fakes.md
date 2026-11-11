# Fakes (lib-test) — catalogue and how to add one

All shared fakes live in the `lib-test` module (`lib-test/src/main/kotlin/no/nav/aap/behandlingsflyt/`), which every
other module has as `testImplementation`. A fake is a working, simple implementation of a production interface. Prefer
it over a mock everywhere; extend it when it lacks something.

## Catalogue

| Need | Fake | Notes |
|---|---|---|
| Repositories in unit tests | `test/inmemoryrepo/InMemory<Name>Repository` (~70) | Mostly `object`s with a `ConcurrentHashMap` → JVM-global state. 8 are classes (new instance per `RepositoryProvider`). Some methods are `TODO()`. |
| `RepositoryProvider` for `konstruer(...)`/secondary constructors | `inMemoryRepositoryProvider`, `inMemoryRepositoryRegistry` (`test/inmemoryrepo/InMemoryRepositoryRegistry.kt`) | Only repositories registered there can be provided. |
| `BehandlingService` | `InMemoryBehandlingService` (`test/inmemoryservice/`) | A real `BehandlingService` over in-memory repos. |
| Sak/behandling setup without DB | `help/opprettSak.kt`: `opprettInMemorySak()`, `opprettInMemorySakOgBehandling()`, `opprettInMemorySakOgRevurdering()` | Random ident, IDs from 10000 upwards. |
| Sak/behandling setup with DB | `help/opprettSak.kt` `opprettSak(connection, dato)`; `help/FinnEllerOpprettBehandling.kt` `sak(connection)`, `finnEllerOpprettBehandling(connection, sak, vurderingsbehov)`, `opprettRevurdering(...)` | For repository/service integration tests. |
| `FlytKontekstMedPerioder`, `AvklaringsbehovKontekst` | `flytKontekstMedPerioder { behandling = …; vurderingType = … }`, `avklaringsbehovKontekst { }` (`help/`) | Defaults: SakId(1)/BehandlingId(1), Revurdering. |
| `TidligereVurderinger` | `FakeTidligereVurderinger(utfall)` (`test/`) | Flags `avslagEllerIngenBehandlingsgrunnlag`, `avslag`, `ingenBehandlingsgrunnlag`. |
| Unleash | `test/FakeUnleash.kt`: `AlleAvskruddUnleash`, `LokalUnleash`, `FakeUnleashBase(map)`, `FakeUnleashBaseWithDefaultDisabled(list)` | `FakeUnleashBase` throws for toggles not in its map. |
| Gateways in unit tests (in-process) | `test/Fake*Gateway.kt` (AnsattInfo, ApiIntern, ArenaOppslag, Dokarkiv, Enhet, Meldekort, Oppgavestyring, PdfGenerator), `test/inmemorygateway/FakePersoninfoGateway`, `FakeTilgangGateway`, `help/FakePdlGateway`; behandlingsflyt tests: `flyt/testutil/FakeBrevbestillingGateway`, `FakeEregGateway` | Register in `createGatewayProvider { register<FakeX>() }` or `minimalGatewayProvider { register<FakeX>() }` (the latter only has Unleash). |
| `BehandlingHendelseService` | `DummyBehandlingHendelseService` | No-op. |
| External systems over HTTP (integration and flyt-tests) | `test/FakeServers.kt` + `test/fakes/<System>Fake.kt` (~33 embedded Ktor servers), started by `@Fakes` | `testGatewayProvider(unleash)` registers the real gateway impls, which find the fakes through `INTEGRASJON_*_URL` system properties. |
| Person data for the HTTP fakes | `FakePersoner.leggTil(TestPerson(...))` (`test/modell/TestPerson.kt`), `TestPersoner.*` and `nyPerson(...)` in flyt-tests | Reset before each test by `@Fakes`. |
| Statistikk events | parameter `hendelser: List<StoppetBehandling>` in a `@Fakes` test | Resolved by `FakesExtension`. |
| `DataSource`/`DBConnection` for code that opens transactions | `MockDataSource()`, `MockConnection().toDBConnection()` | Hand-written no-op JDBC stubs; any real SQL hits `TODO()`. Use with in-memory repos only. |
| Dates/clock | `test/Dato.kt` (`1 januar 2025`), `test/Klokke.kt` (`fixedClock(dato)`) | |
| Asserting a `Tidslinje` | `help/AssertTidslinje.kt` (`assertTidslinje`, `assertTidslinjeEquals`) | |

## Typical unit test wiring

```kotlin
// production constructor + fakes, no mocks
val steg = SendForvaltningsmeldingSteg.konstruer(
    inMemoryRepositoryProvider,
    createGatewayProvider {
        register<FakeBrevbestillingGateway>()
        register<FakeOppgavestyringGateway>()
        register<AlleAvskruddUnleash>()
    },
)
val (sak, behandling) = opprettInMemorySakOgBehandling()
```

or with the primary constructor: `FastsettMeldeperiodeSteg(InMemoryMeldeperiodeRepository, FakeTidligereVurderinger(),
InMemorySakRepository)`.

## Adding a fake

**In-memory repository** (needed when unit tests use a new repository interface):
1. `lib-test/.../test/inmemoryrepo/InMemory<Name>Repository.kt`: `object InMemory<Name>Repository : <Name>Repository`
   with a `ConcurrentHashMap<BehandlingId, …>`; implement `kopier` (copy the reference) and `slett` (remove) too.
   An `object` needs no factory; a `class` needs `companion object : RepositoryFactory<<Name>Repository> {
   override fun konstruer(connection: DBConnection) = InMemory<Name>Repository() }`.
2. Register it in `inMemoryRepositoryRegistry` (`InMemoryRepositoryRegistry.kt`).
3. `python3 .agents/skills/repository-og-migrering/scripts/repo.py oversikt` shows which repositories have one (column MEM).

**In-process gateway fake**: `class Fake<Name>Gateway : <Name>Gateway { … companion object : Factory<<Name>Gateway> {
override fun konstruer() = Fake<Name>Gateway() } }` (or an `object`) in `lib-test/.../test/`; register it per test with
`createGatewayProvider { register<Fake<Name>Gateway>() }`.

**HTTP fake for a new integration** (needed by integration and flyt-tests):
1. `lib-test/.../test/fakes/<System>Fake.kt`: `class <System>Fake(private val fakePersoner: () -> TestPersonService) :
   FakeServer()` with `override val server = embeddedServer(Netty, port = 0, module = module())`,
   `installerContentNegotiation()`, `installerStatusPages("<SYSTEM>")` and routes answering from `fakePersoner()`
   (see `DagpengerFake`). Add data fields to `TestPerson` if needed.
2. In `FakeServers.kt`: create it, add it to `allFakes`, and in `setProperties()` set
   `INTEGRASJON_<SYSTEM>_URL` = `http://localhost:${x.port()}` (+ `_SCOPE`) with the same keys the gateway reads.
3. Register the real gateway impl in `testGatewayProvider()` (`test/TestGatewayProvider.kt`) and in production
   `defaultGatewayProvider()`.

**New feature toggle**: add it to `LokalUnleash` (otherwise `feature toggle X ikke definert for fake`);
`AlleAvskruddUnleash` needs nothing (everything off).
