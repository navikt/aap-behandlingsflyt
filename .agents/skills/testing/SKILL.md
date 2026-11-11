---
name: testing
description: Testing aap-behandlingsflyt — the difference between unit tests, integration tests and flyt-tests (AbstraktFlytOrkestratorTest) and which one to write, fakes over mocks (in-memory repositories, fake gateways, FakeServers, fake Unleash; MockK only sparingly in unit tests), house style, and running tests safely with ./gradlew — never the whole suite, one class or a small batch per run, flyt-tests in small batches to avoid timeouts. Use whenever you write, change, run or debug tests, or need to verify a change. Keywords — test, enhetstest, integrasjonstest, flyttest, fake, mock, mockk, InMemory, TestDataSource, testcontainers, gradlew, timeout, kjørJobber.
---

# Testing

## Rules (read first)

1. **Never run the whole suite.** Not `./gradlew test`/`check`/`build`, and not an unfiltered `:behandlingsflyt:test`,
   `:repository:test`, `:api:test` or `:app:test`. It is slow and times out in agent sandboxes; CI runs everything
   (one job per module). Unfiltered is fine only for the tiny `:kontrakt:test` and `:lib-test:test`.
2. **Always `./gradlew`** from the repo root, never a globally installed `gradle` (the wrapper pins the Gradle version
   the build is made for, with a checksum).
3. **Run the narrowest set**: one class (or method) per invocation, module-scoped with `--tests`. Need many tests? Split
   them over several invocations, **one per tool call**, and read each result before starting the next.
   **Flyt-tests: at most ~3 classes / ~20 tests per run** — they are what makes runs time out (all 34 flyt classes in one
   run took 5 min wall and 16 min of test time on an 18-core laptop; a sandbox is much slower).
4. One test command at a time: parallel runs compete for CPU and Docker (every test JVM starts its own Postgres) and
   overwrite each other's results. Give the command a generous tool timeout (≥ 10 min) or run it in the background and
   wait for the notification. The first run also compiles behandlingsflyt + repository + lib-test.
5. **Fakes over mocks** (see below). No mocks in integration or flyt-tests.
6. Say exactly what you ran and the result. If you could not run tests (no JDK/Docker, timeout), say so; never claim
   tests pass.

Helper (Python 3, no build): `python3 .agents/skills/testing/scripts/kjor_test.py` — prints/runs the right command,
batches, lists tests by kind, suggests tests for changed files and summarises results (`-h` for all options).

## Unit tests, integration tests and flyt-tests

| | Unit test (enhetstest) | Integration test (integrasjonstest) | Flyt-test (flyttest) |
|---|---|---|---|
| Exercises | one class or pure function | one component against real infrastructure | the whole engine: a behandling through its steps |
| Infrastructure | none; fakes in memory | Postgres (`TestDataSource`), HTTP fakes (`@Fakes`), Ktor `testApplication`, Kafka | Postgres + all repositories + real gateways vs `FakeServers` + `ManuellMotorImpl` |
| Docker | no | for Postgres/Kafka | yes |
| Cost | ms (a whole module's unit tests: seconds) | ~0.3–8 s per class; `app` server tests up to ~2 min | 4–70 s per class |
| Count | ~200 classes | ~145 classes | 34 classes (`kjor_test.py --liste flyt`) |

**Unit tests** — rules, vilkår, `Tidslinje` logic, beregning, mappers, and single steps/løsere/services/jobs built
with the production constructor and fakes. No `@Fakes`, no `TestDataSource`.
- Pure: `meldeplikt/MeldepliktRegelTest`, `behandling/underveis/regler/*`, `behandling/beregning/BeregningTest`
  (`@TableTest`), all of `kontrakt/src/test`.
- With fakes: `meldeplikt/FastsettMeldeperiodeStegTest` (in-memory repos + `FakeTidligereVurderinger`),
  `forretningsflyt/steg/SykestipendStegTest` (`opprettInMemorySakOgBehandling()`, `inMemoryRepositoryProvider`,
  `minimalGatewayProvider()`), `forretningsflyt/steg/SendForvaltningsmeldingStegTest`
  (`SendForvaltningsmeldingSteg.konstruer(inMemoryRepositoryProvider, createGatewayProvider { register<FakeX>() })`),
  `flyt/EnklereFlytOrkestratorTest` (the engine on in-memory repos with dummy steps).

**Integration tests** — the boundary of one component with real infrastructure:
- Repository/SQL: `repository/src/test/.../*RepositoryImplTest` (a few live in behandlingsflyt). `TestDataSource()` in a
  companion `@BeforeAll`, `close()` in `@AfterAll`, each test inside `dataSource.transaction { connection -> … }`.
- Gateway/HTTP client: `repository/src/test/.../integrasjon/*GatewayTest` with `@Fakes`: the real gateway impl calls an
  embedded fake server; put data in with `FakePersoner.leggTil(TestPerson(...))`.
- Service + database: `AvklaringsbehovOrkestratorTest`, `StegOrkestratorTest`, `api/.../SakOgBehandlingServiceTest`
  (`postgresRepositoryRegistry.provider(connection)`).
- API routes (`api/src/test`, no Docker): `BaseApiTest` + `@Fakes` (token) + `testApplication { installApplication {
  xApi(MockDataSource(), inMemoryRepositoryRegistry, gatewayProvider) } }` — e.g. `UnderveisGrunnlagApiTest`.
- App (`app/src/test`, heaviest — treat like flyt-tests): Kafka consumers (`SharedKafkaTestContainer`, `awaitAtMost`);
  `ApiTest`, `OpprettOgFullførBehandlingApiTest` (~2 min alone), `PipTest` start the whole server on a random port with
  their own Postgres.

**Flyt-tests** — `class X : AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class)` (or
`AbstraktFlytOrkestratorSnapshotTest`), mostly in `behandlingsflyt/src/test/.../flyt/`. One database per class, all
tables truncated before each test, `@Fakes`, jobs run synchronously by `motor.kjørJobber()`. Drive a behandling with
the DSL (`sendInnFørsteSøknad`, `løsSykdom`, `kvalitetssikre`, `fattVedtak`, `medKontekst { … }`). Use them for
behaviour across steps: which avklaringsbehov appear, stop points, rewinds, revurdering, vedtak, brev. Don't use them
for logic a unit test can cover. DSL, snapshot tests and pitfalls: [references/flyttester.md](references/flyttester.md).

## Which test for which change

| Change | Write / extend | Then run |
|---|---|---|
| regel, vilkår, beregning, mapper | unit test | that class |
| step logic (`utfør`, `utledBehov`) | unit test with fakes | it + the closest flyt-test |
| løsning/løser | unit test with in-memory repos | it + the flyt-test that solves the behov |
| repository/SQL/migration | repository integration test (lagre/hent, kopier, slett) | that class |
| gateway | gateway test against a fake server | that class |
| API route/DTO | api test (in-memory) | that class |
| flow, step order, avklaringsbehov behaviour | flyt-test (extend an existing class if it fits) | 1–2 related flyt-tests |
| job (`JobbUtfører`) | unit test (`InMemoryFlytJobbRepository`); flyt-test if part of a flow | that class |
| Kafka consumer | app test | that class |

Find candidates: `kjor_test.py --endret` (uncommitted changes; `--endret origin/main` for everything on your branch).

## Fakes over mocks

Use fakes (working in-memory implementations) for collaborators, in unit tests too. Catalogue and how to add one:
[references/fakes.md](references/fakes.md). The usual swaps:

| Instead of mocking | Use (lib-test unless noted) |
|---|---|
| a repository | `InMemory<Name>Repository`, or `XSteg.konstruer(inMemoryRepositoryProvider, gatewayProvider)` |
| `BehandlingService`, `SakService` | `InMemoryBehandlingService`, `SakService(InMemorySakRepository, InMemoryBehandlingRepository)` |
| `TidligereVurderinger` | `FakeTidligereVurderinger()` (set `avslagEllerIngenBehandlingsgrunnlag` etc.) |
| `UnleashGateway` | `AlleAvskruddUnleash`, `LokalUnleash`, `object : FakeUnleashBaseWithDefaultDisabled(listOf(...))` |
| a gateway | `Fake*Gateway` registered in `createGatewayProvider { }`; HTTP fakes + `testGatewayProvider()` in integration tests |
| `DataSource`/`DBConnection` with in-memory repos | `MockDataSource()` / `MockConnection().toDBConnection()` (no-op JDBC stubs) |
| domain objects (`Sak`, `Underveisperiode`, …) | construct them; builders `flytKontekstMedPerioder { }`, `opprettInMemorySakOgBehandling()` |

MockK (`io.mockk`, on the test classpath of behandlingsflyt, api and repository) appears in ~50 unit tests. Don't
copy that pattern. A mock is acceptable **only in a unit test**, when the collaborator has no fake and cannot reasonably
be built from fakes, and you need just a canned answer or to verify a call with no observable state. Then: mock the
narrowest interface, no `relaxed = true`, no `mockkObject`/`mockkStatic` (JVM-global), never mock data classes. Needing
the same mock twice means it is time to add a fake to lib-test. When extending an existing mock-based class, follow its
setup rather than mixing styles, but add no new mocks. Don't rewrite existing tests unless asked.

## Running

- One class: `./gradlew :behandlingsflyt:test --tests 'no.nav.aap.behandlingsflyt.flyt.AlderFlytTest'`
  (`kjor_test.py AlderFlytTest` finds module and package). Tests live in the module of the code they test.
- One method: `--tests 'no.nav.aap.behandlingsflyt.flyt.AlderFlytTest.*rett under 62*'` (matches the method name;
  backtick names contain spaces, so quote) — `kjor_test.py AlderFlytTest -m "rett under 62"`.
- Several classes: repeat `--tests` (`kjor_test.py A B C` groups per module and batches flyt-tests); a package:
  `--tests 'no.nav.aap.behandlingsflyt.behandling.underveis.*'` (only where it holds no flyt-tests).
- Many tests: `kjor_test.py --batch enhet|integrasjon|flyt [--modul m]` prints numbered commands (unit tests in one,
  integration tests ≤ 40 classes, flyt-tests and `app` integration tests ≤ 3 classes/20 tests per command; tune with
  `--maks-klasser`/`--maks-tester`);
  `kjor_test.py --batch flyt --run 3` runs number 3. One per tool call; fix failures before the next batch, and iterate
  on the failing class or method (`-m`) rather than re-running the batch.
- Compile only (cheap check, no tests): `./gradlew :behandlingsflyt:compileTestKotlin`; all modules: `./gradlew compileTestKotlin`.
- `--run` writes the full Gradle output to a temp log and prints a short summary. `kjor_test.py --resultat [modul]`
  summarises `<modul>/build/test-results/test/TEST-*.xml` (failures, slowest classes, **swallowed motor job errors**).
  A module's previous XML results are deleted each time its test task runs.
- Gradle's build cache is on and tests use app version `development`: an unchanged test task is `UP-TO-DATE`/`FROM-CACHE`
  and **does not run**. Add `--rerun` to force it.
- Prerequisites: a JDK (the build uses a JDK 25 toolchain) and Docker for Postgres/Kafka (Colima env vars in README).
  `kjor_test.py --sjekk-miljo`. Without Docker you can still run unit tests and the integration tests that only use
  `@Fakes`/`testApplication` (`kjor_test.py --liste integrasjon` marks them `(no docker)`) — say what you could not run.
- Measured on an 18-core Mac (a sandbox is several times slower): first run incl. compilation ~1.5 min; all ~170 unit
  classes of behandlingsflyt in one run 7 s; all 87 classes of `:repository` 27 s; the 11 `app` integration classes
  2.4 min; flyt classes 4–70 s each (`InstitusjonFlytTest`, 17 tests, 69 s; `AlderFlytTest` 21 s incl. Postgres start);
  all 34 flyt classes 5 min.
  Gradle runs ≤ 4 test JVMs locally (all cores on CI), each with its own Postgres container; JUnit's default timeout is
  3 min per test; TZ is Europe/Oslo.

## House style

- JUnit 5 (`org.junit.jupiter.api.Test`) and AssertJ `assertThat` (dominant); test names are backtick sentences in
  Norwegian: ``fun `stopper ikke opp når søker er rett under 62 år gammel`()``.
- Same module and package as the code under test (feature packages `no.nav.aap.<feature>` too).
- Dates: `1 januar 2024` (`lib-test/.../test/Dato.kt`), `fixedClock(dato)` for code taking a `Clock`; flyt-tests use
  dates relative to `LocalDate.now()` so rules about age, deadlines and meldeperioder keep working.
- Parametrise with `@ParameterizedTest` + `@EnumSource`/`@MethodSource`, `@TableTest` for table-driven cases, and
  `@ParameterizedClass @MethodSource("unleashTestDataSource")` to run a flyt-test with both fake Unleash setups (doubles
  its run time; only when toggles change behaviour).
- Skeletons for each kind: [references/maler.md](references/maler.md).

## Pitfalls (verified)

- In-memory repositories are mostly Kotlin `object`s: **state is shared by all tests in the JVM**. Create fresh
  sak/behandling per test (`opprettInMemorySakOgBehandling()`, IDs from 10000 upwards), never assume an empty repo.
  Eight are classes (new instance per `RepositoryProvider`); some fake methods are `TODO()` — implement them in lib-test.
- Repository tests share one database per class (no truncation): every test creates its own sak/behandling.
- `Repository av typen 'X' er ikke registrert` / `Gateway av typen 'X' er ikke registrert` → register the fake in
  `inMemoryRepositoryRegistry` / your `createGatewayProvider { }` (or `testGatewayProvider()` + `FakeServers`).
- `feature toggle X ikke definert for fake` → add the toggle to `LokalUnleash` (lib-test `FakeUnleash.kt`).
- Flyt-test fails strangely → `ManuellMotorImpl` swallowed a job exception (`Feil under prosessering av jobb`);
  `kjor_test.py --resultat` lists them. More in [references/flyttester.md](references/flyttester.md).
- `Could not find a valid Docker environment` → Docker/Colima not running or env vars missing.
- `No tests found for given includes` → wrong module or a filter that matches nothing.
