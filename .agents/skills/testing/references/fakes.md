# Fakes (lib-test): katalog og hvordan du legger til en

Alle delte fakes ligger i modulen `lib-test` (`lib-test/src/main/kotlin/no/nav/aap/behandlingsflyt/`), som alle andre
moduler har som `testImplementation`. En fake er en enkel, fungerende implementasjon av et produksjonsgrensesnitt. Foretrekk
den framfor en mock overalt, og utvid den hvis den mangler noe.

## Katalog

| Behov | Fake | Merknader |
|---|---|---|
| Repositories i enhetstester | `test/inmemoryrepo/InMemory<Name>Repository` (~70) | For det meste `object`-er med en `ConcurrentHashMap` → tilstand global for JVM-en. 8 er klasser (ny instans per `RepositoryProvider`). Noen metoder er `TODO()`. |
| `RepositoryProvider` for `konstruer(...)`/sekundærkonstruktører | `inMemoryRepositoryProvider`, `inMemoryRepositoryRegistry` (`test/inmemoryrepo/InMemoryRepositoryRegistry.kt`) | Bare repositories som er registrert der, kan hentes. |
| `BehandlingService` | `InMemoryBehandlingService` (`test/inmemoryservice/`) | En ekte `BehandlingService` som bruker in-memory repositories. |
| Oppsett av sak/behandling uten DB | `help/opprettSak.kt`: `opprettInMemorySak()`, `opprettInMemorySakOgBehandling()`, `opprettInMemorySakOgRevurdering()` | Tilfeldig ident, ID-er fra 10000 og oppover. |
| Oppsett av sak/behandling med DB | `help/opprettSak.kt` `opprettSak(connection, dato)`; `help/FinnEllerOpprettBehandling.kt` `sak(connection)`, `finnEllerOpprettBehandling(connection, sak, vurderingsbehov)`, `opprettRevurdering(...)` | For integrasjonstester av repository/tjeneste. |
| `FlytKontekstMedPerioder`, `AvklaringsbehovKontekst` | `flytKontekstMedPerioder { behandling = …; vurderingType = … }`, `avklaringsbehovKontekst { }` (`help/`) | Standardverdier: SakId(1)/BehandlingId(1), Revurdering. |
| `TidligereVurderinger` | `FakeTidligereVurderinger(utfall)` (`test/`) | Flaggene `avslagEllerIngenBehandlingsgrunnlag`, `avslag`, `ingenBehandlingsgrunnlag`. |
| Unleash | `test/FakeUnleash.kt`: `AlleAvskruddUnleash`, `LokalUnleash`, `FakeUnleashBase(map)`, `FakeUnleashBaseWithDefaultDisabled(list)` | `FakeUnleashBase` kaster feil for toggles som ikke finnes i map-et. |
| Gateways i enhetstester (i samme prosess) | `test/Fake*Gateway.kt` (AnsattInfo, ApiIntern, ArenaOppslag, Dokarkiv, Enhet, Meldekort, Oppgavestyring, PdfGenerator), `test/inmemorygateway/FakePersoninfoGateway`, `FakeTilgangGateway`, `help/FakePdlGateway`; tester i behandlingsflyt: `flyt/testutil/FakeBrevbestillingGateway`, `FakeEregGateway` | Registrer i `createGatewayProvider { register<FakeX>() }` eller `minimalGatewayProvider { register<FakeX>() }` (den siste har bare Unleash). |
| `BehandlingHendelseService` | `DummyBehandlingHendelseService` | Gjør ingenting. |
| Eksterne systemer over HTTP (integrasjons- og flyttester) | `test/FakeServers.kt` + `test/fakes/<System>Fake.kt` (~33 innebygde Ktor-servere), startet av `@Fakes` | `testGatewayProvider(unleash)` registrerer de ekte gateway-implementasjonene, som finner fake-ene via systemegenskapene `INTEGRASJON_*_URL`. |
| Persondata for HTTP-fake-ene | `FakePersoner.leggTil(TestPerson(...))` (`test/modell/TestPerson.kt`), `TestPersoner.*` og `nyPerson(...)` i flyttester | Nullstilles før hver test av `@Fakes`. |
| Statistikkhendelser | Parameteren `hendelser: List<StoppetBehandling>` i en `@Fakes`-test | Håndteres av `FakesExtension`. |
| `DataSource`/`DBConnection` for kode som åpner transaksjoner | `MockDataSource()`, `MockConnection().toDBConnection()` | Håndskrevne JDBC-stubber uten effekt; faktisk SQL gir `TODO()`. Bruk bare med in-memory repositories. |
| Datoer/klokke | `test/Dato.kt` (`1 januar 2025`), `test/Klokke.kt` (`fixedClock(dato)`) | |
| Sjekke en `Tidslinje` | `help/AssertTidslinje.kt` (`assertTidslinje`, `assertTidslinjeEquals`) | |

## Typisk oppsett for enhetstest

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

eller med primærkonstruktøren: `FastsettMeldeperiodeSteg(InMemoryMeldeperiodeRepository, FakeTidligereVurderinger(),
InMemorySakRepository)`.

## Legge til en fake

**In-memory repository** (trengs når enhetstester bruker et nytt repository-grensesnitt):
1. `lib-test/.../test/inmemoryrepo/InMemory<Name>Repository.kt`: `object InMemory<Name>Repository : <Name>Repository`
   med en `ConcurrentHashMap<BehandlingId, …>`. Implementer også `kopier` (kopier referansen) og `slett` (fjern).
   Et `object` trenger ingen factory; en `class` trenger `companion object : RepositoryFactory<<Name>Repository> {
   override fun konstruer(connection: DBConnection) = InMemory<Name>Repository() }`.
2. Registrer den i `inMemoryRepositoryRegistry` (`InMemoryRepositoryRegistry.kt`).
3. `python3 .agents/skills/repository-og-migrering/scripts/repo.py oversikt` viser hvilke repositories som har en (kolonnen MEM).

**Gateway-fake i samme prosess**: `class Fake<Name>Gateway : <Name>Gateway { … companion object : Factory<<Name>Gateway> {
 override fun konstruer() = Fake<Name>Gateway() } }` (eller et `object`) i `lib-test/.../test/`. Registrer den for hver test med
`createGatewayProvider { register<Fake<Name>Gateway>() }`.

**HTTP-fake for en ny integrasjon** (trengs i integrasjons- og flyttester):
1. `lib-test/.../test/fakes/<System>Fake.kt`: `class <System>Fake(private val fakePersoner: () -> TestPersonService) :
   FakeServer()` med `override val server = embeddedServer(Netty, port = 0, module = module())`,
   `installerContentNegotiation()`, `installerStatusPages("<SYSTEM>")` og ruter som svarer med data fra `fakePersoner()`
   (se `DagpengerFake`). Legg til datafelter i `TestPerson` hvis det trengs.
2. I `FakeServers.kt`: opprett den, legg den til i `allFakes`, og sett i `setProperties()`
   `INTEGRASJON_<SYSTEM>_URL` = `http://localhost:${x.port()}` (+ `_SCOPE`) med de samme nøklene som gatewayen leser.
3. Registrer den ekte gateway-implementasjonen i `testGatewayProvider()` (`test/TestGatewayProvider.kt`) og i produksjonens
   `defaultGatewayProvider()`.

**Ny feature toggle**: legg den til i `LokalUnleash` (ellers får du `feature toggle X ikke definert for fake`);
`AlleAvskruddUnleash` trenger ingen endring (alt er slått av).
