---
name: testing
description: Testing i aap-behandlingsflyt: forskjellen på enhetstester, integrasjonstester og flyttester (AbstraktFlytOrkestratorTest), og hvilken du bør skrive, fakes framfor mocks (in-memory repositories, fake gateways, FakeServers, fake Unleash; bruk MockK sparsomt i enhetstester), kodestil og trygg kjøring av tester med ./gradlew. Kjør aldri hele testsettet, men én klasse eller en liten gruppe per kjøring. Kjør flyttester i små grupper for å unngå tidsavbrudd. Bruk denne skillen når du skriver, endrer, kjører eller feilsøker tester, eller trenger å verifisere en endring. Nøkkelord — test, enhetstest, integrasjonstest, flyttest, fake, mock, mockk, InMemory, TestDataSource, testcontainers, gradlew, timeout, kjørJobber.
---

# Testing

## Regler (les først)

1. **Kjør aldri hele testsettet.** Ikke `./gradlew test`/`check`/`build` og ikke en ufiltrert `:behandlingsflyt:test`,
   `:repository:test`, `:api:test` eller `:app:test`. Det tar lang tid og gir tidsavbrudd i agent-sandkasser. CI kjører alt
   (én jobb per modul). Ufiltrert kjøring er bare greit for de små `:kontrakt:test` og `:lib-test:test`.
2. **Bruk alltid `./gradlew`** fra repo-roten, aldri en globalt installert `gradle` (wrapperen låser Gradle-versjonen
   som bygget bruker, med en sjekksum).
3. **Kjør det minste relevante utvalget**: én klasse (eller metode) per kjøring, avgrenset til en modul med `--tests`. Må du kjøre mange tester? Del
   dem opp i flere kjøringer, **én per verktøykall**, og les resultatet før du starter neste.
   **Flyttester: maksimalt ~3 klasser / ~20 tester per kjøring**. De får kjøringer til å gå over tidsgrensen (alle 34 flyttestklassene i én
   kjøring tok 5 min veggtid og 16 min testtid på en bærbar med 18 kjerner; en sandkasse er mye tregere).
4. Kjør én testkommando om gangen. Parallelle kjøringer konkurrerer om CPU og Docker (hver test-JVM starter sin egen Postgres)
   og overskriver hverandres resultater. Sett en raus tidsgrense for verktøyet (≥ 10 min), eller kjør kommandoen i bakgrunnen og
   vent på varselet. Første kjøring kompilerer også behandlingsflyt + repository + lib-test.
5. **Bruk fakes framfor mocks** (se nedenfor). Ikke bruk mocks i integrasjons- eller flyttester.
6. Oppgi nøyaktig hva du kjørte, og resultatet. Hvis du ikke kunne kjøre testene (manglende JDK/Docker, tidsavbrudd), si det. Ikke påstå
   at testene består.

Hjelpeverktøy (Python 3, trenger ikke bygge): `python3 .agents/skills/testing/scripts/kjor_test.py` skriver ut eller kjører riktig kommando,
deler opp kjøringer, viser tester etter type, foreslår tester for endrede filer og oppsummerer resultater (`-h` viser alle alternativene).

## Enhetstester, integrasjonstester og flyttester

| | Enhetstest | Integrasjonstest | Flyttest |
|---|---|---|---|
| Tester | én klasse eller ren funksjon | én komponent mot ekte infrastruktur | hele motoren: en behandling gjennom stegene |
| Infrastruktur | ingen; fakes i minnet | Postgres (`TestDataSource`), HTTP-fakes (`@Fakes`), Ktor `testApplication`, Kafka | Postgres + alle repositories + ekte gateways mot `FakeServers` + `ManuellMotorImpl` |
| Docker | nei | for Postgres/Kafka | ja |
| Tidsbruk | ms (en modul med enhetstester: sekunder) | ~0,3–8 s per klasse; servertester i `app` opptil ~2 min | 4–70 s per klasse |
| Antall | ~200 klasser | ~145 klasser | 34 klasser (`kjor_test.py --liste flyt`) |

**Enhetstester**: regler, vilkår, `Tidslinje`-logikk, beregning, mappere og enkeltsteg/løsere/tjenester/jobber bygd
med produksjonskonstruktøren og fakes. Ingen `@Fakes` eller `TestDataSource`.
- Rene tester: `meldeplikt/MeldepliktRegelTest`, `behandling/underveis/regler/*`, `behandling/beregning/BeregningTest`
  (`@TableTest`) og alt i `kontrakt/src/test`.
- Med fakes: `meldeplikt/FastsettMeldeperiodeStegTest` (in-memory repositories + `FakeTidligereVurderinger`),
  `forretningsflyt/steg/SykestipendStegTest` (`opprettInMemorySakOgBehandling()`, `inMemoryRepositoryProvider`,
  `minimalGatewayProvider()`), `forretningsflyt/steg/SendForvaltningsmeldingStegTest`
  (`SendForvaltningsmeldingSteg.konstruer(inMemoryRepositoryProvider, createGatewayProvider { register<FakeX>() })`),
  `flyt/EnklereFlytOrkestratorTest` (motoren med in-memory repositories og dummy-steg).

**Integrasjonstester**: grensesnittet til én komponent mot ekte infrastruktur:
- Repository/SQL: `repository/src/test/.../*RepositoryImplTest` (noen ligger i behandlingsflyt). Opprett `TestDataSource()` i
  et companion-objekt med `@BeforeAll`, kall `close()` i `@AfterAll`, og kjør hver test i `dataSource.transaction { connection -> … }`.
- Gateway/HTTP-klient: `repository/src/test/.../integrasjon/*GatewayTest` med `@Fakes`: den ekte gateway-implementasjonen kaller en
  innebygd fake-server. Legg inn data med `FakePersoner.leggTil(TestPerson(...))`.
- Tjeneste + database: `AvklaringsbehovOrkestratorTest`, `StegOrkestratorTest`, `api/.../SakOgBehandlingServiceTest`
  (`postgresRepositoryRegistry.provider(connection)`).
- API-ruter (`api/src/test`, uten Docker): `BaseApiTest` + `@Fakes` (token) + `testApplication { installApplication {
  xApi(MockDataSource(), inMemoryRepositoryRegistry, gatewayProvider) } }`, for eksempel `UnderveisGrunnlagApiTest`.
- App (`app/src/test`, tyngst, behandle som flyttester): Kafka-konsumenter (`SharedKafkaTestContainer`, `awaitAtMost`);
  `ApiTest`, `OpprettOgFullførBehandlingApiTest` (~2 min alene) og `PipTest` starter hele serveren på en tilfeldig port med
  hver sin Postgres.

**Flyttester**: `class X : AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class)` (eller
`AbstraktFlytOrkestratorSnapshotTest`), hovedsakelig i `behandlingsflyt/src/test/.../flyt/`. Én database per klasse, alle
tabeller tømmes før hver test, `@Fakes`, jobber kjøres synkront med `motor.kjørJobber()`. Kjør en behandling gjennom
DSL-en (`sendInnFørsteSøknad`, `løsSykdom`, `kvalitetssikre`, `fattVedtak`, `medKontekst { … }`). Bruk dem til å teste
atferd på tvers av steg: hvilke avklaringsbehov som oppstår, stoppunkter, tilbakeføringer, revurdering, vedtak og brev. Ikke bruk dem
for logikk en enhetstest kan dekke. DSL, snapshot-tester og fallgruver: [references/flyttester.md](references/flyttester.md).

## Hvilken test for hvilken endring

| Endring | Skriv / utvid | Kjør deretter |
|---|---|---|
| regel, vilkår, beregning, mapper | enhetstest | den klassen |
| steglogikk (`utfør`, `utledBehov`) | enhetstest med fakes | den + nærmeste flyttest |
| løsning/løser | enhetstest med in-memory repositories | den + flyttesten som løser behovet |
| repository/SQL/migrering | repository-integrasjonstest (lagre/hent, kopier, slett) | den klassen |
| gateway | gateway-test mot en fake-server | den klassen |
| API-rute/DTO | API-test (in-memory) | den klassen |
| flyt, rekkefølge på steg, atferd for avklaringsbehov | flyttest (utvid en eksisterende klasse hvis den passer) | 1–2 relaterte flyttester |
| job (`JobbUtfører`) | enhetstest (`InMemoryFlytJobbRepository`); flyttest hvis den inngår i en flyt | den klassen |
| Kafka-konsument | app-test | den klassen |

Finn aktuelle tester med `kjor_test.py --endret` (ikke-committede endringer; `--endret origin/main` for alt på branchen din).

## Fakes framfor mocks

Bruk fakes (fungerende implementasjoner i minnet) for samarbeidende komponenter, også i enhetstester. Se katalogen og
hvordan du legger til en fake: [references/fakes.md](references/fakes.md). Vanlige erstatninger:

| I stedet for mocking | Bruk (fra lib-test hvis ikke annet er oppgitt) |
|---|---|
| et repository | `InMemory<Name>Repository` eller `XSteg.konstruer(inMemoryRepositoryProvider, gatewayProvider)` |
| `BehandlingService`, `SakService` | `InMemoryBehandlingService`, `SakService(InMemorySakRepository, InMemoryBehandlingRepository)` |
| `TidligereVurderinger` | `FakeTidligereVurderinger()` (sett for eksempel `avslagEllerIngenBehandlingsgrunnlag`) |
| `UnleashGateway` | `AlleAvskruddUnleash`, `LokalUnleash`, `object : FakeUnleashBaseWithDefaultDisabled(listOf(...))` |
| en gateway | `Fake*Gateway` registrert i `createGatewayProvider { }`; HTTP-fakes + `testGatewayProvider()` i integrasjonstester |
| `DataSource`/`DBConnection` med in-memory repositories | `MockDataSource()` / `MockConnection().toDBConnection()` (JDBC-stubber uten effekt) |
| domeneobjekter (`Sak`, `Underveisperiode`, …) | opprett dem; bruk byggerne `flytKontekstMedPerioder { }`, `opprettInMemorySakOgBehandling()` |

MockK (`io.mockk`, på test-classpathen til behandlingsflyt, api og repository) brukes i rundt 50 enhetstester. Ikke
kopier den praksisen. En mock kan brukes **bare i en enhetstest** når samarbeidspartneren ikke har en fake og ikke med rimelighet
kan bygges med fakes, og du bare trenger et forhåndsdefinert svar eller vil verifisere et kall uten observerbar tilstand. Da skal du mocke
det smaleste grensesnittet, ikke bruke `relaxed = true` eller `mockkObject`/`mockkStatic` (globalt i JVM), og aldri mocke dataklasser. Hvis du trenger
samme mock to ganger, er det på tide å legge til en fake i lib-test. Når du utvider en eksisterende klasse som bruker mocks, følg oppsettet
i stedet for å blande stiler, men ikke legg til nye mocks. Ikke skriv om eksisterende tester med mindre noen ber om det.

## Kjøre tester

- Én klasse: `./gradlew :behandlingsflyt:test --tests 'no.nav.aap.behandlingsflyt.flyt.AlderFlytTest'`
  (`kjor_test.py AlderFlytTest` finner modul og pakke). Testene ligger i modulen til koden de tester.
- Én metode: `--tests 'no.nav.aap.behandlingsflyt.flyt.AlderFlytTest.*rett under 62*'` (matcher metodenavnet;
  navn med backticks inneholder mellomrom og må derfor stå i anførselstegn) — `kjor_test.py AlderFlytTest -m "rett under 62"`.
- Flere klasser: gjenta `--tests` (`kjor_test.py A B C` grupperer per modul og deler opp flyttester); en pakke:
  `--tests 'no.nav.aap.behandlingsflyt.behandling.underveis.*'` (bare hvis den ikke inneholder flyttester).
- Mange tester: `kjor_test.py --batch enhet|integrasjon|flyt [--modul m]` skriver ut nummererte kommandoer (enhetstester i én,
  integrasjonstester med ≤ 40 klasser, flyttester og integrasjonstester i `app` med ≤ 3 klasser/20 tester per kommando. Juster med
  `--maks-klasser`/`--maks-tester`);
  `kjor_test.py --batch flyt --run 3` kjører gruppe 3. Kjør én gruppe per verktøykall. Rett feil før neste gruppe, og gjenta
  den feilede klassen eller metoden (`-m`) i stedet for hele gruppen.
- Bare kompilering (rask sjekk, kjører ikke tester): `./gradlew :behandlingsflyt:compileTestKotlin`; alle moduler: `./gradlew compileTestKotlin`.
- `--run` skriver hele Gradle-utdataene til en midlertidig logg og viser en kort oppsummering. `kjor_test.py --resultat [modul]`
  oppsummerer `<modul>/build/test-results/test/TEST-*.xml` (feil, tregeste klasser, **motorjobbfeil som ble fanget opp**).
  Testoppgaven sletter modulens tidligere XML-resultater hver gang den kjører.
- Gradles byggbuffer er aktiv, og testene bruker app-versjonen `development`: En uendret testoppgave får status `UP-TO-DATE`/`FROM-CACHE`
  og **kjører ikke**. Legg til `--rerun` for å tvinge fram en ny kjøring.
- Forutsetninger: JDK (bygget bruker JDK 25 toolchain) og Docker for Postgres/Kafka (Colima-miljøvariabler i README).
  Kjør `kjor_test.py --sjekk-miljo`. Uten Docker kan du fortsatt kjøre enhetstester og integrasjonstester som bare bruker
  `@Fakes`/`testApplication` (`kjor_test.py --liste integrasjon` markerer dem med `(no docker)`). Oppgi hva du ikke kunne kjøre.
- Målt på en Mac med 18 kjerner (en sandkasse er flere ganger tregere): første kjøring med kompilering tar rundt 1,5 min; alle ~170 enhetstest-
  klasser i behandlingsflyt tar 7 s i én kjøring; alle 87 klassene i `:repository` tar 27 s; de 11 integrasjonsklassene i `app` tar
  2,4 min; hver flyttestklasse tar 4–70 s (`InstitusjonFlytTest`, 17 tester, 69 s; `AlderFlytTest`, 21 tester inkludert oppstart av Postgres);
  alle 34 flyttestklassene tar 5 min.
  Gradle kjører lokalt ≤ 4 test-JVM-er (alle kjerner i CI), hver med sin egen Postgres-container. JUnits standard tidsgrense er
  3 min per test; tidssone er Europe/Oslo.

## Kodestil for tester

- JUnit 5 (`org.junit.jupiter.api.Test`) og AssertJ `assertThat` (mest brukt); testnavn er setninger på norsk i backticks:
  ``fun `stopper ikke opp når søker er rett under 62 år gammel`()``.
- Bruk samme modul og pakke som koden testen dekker (også feature-pakker som `no.nav.aap.<feature>`).
- Datoer: `1 januar 2024` (`lib-test/.../test/Dato.kt`), `fixedClock(dato)` for kode som tar en `Clock`; flyttester bruker
  datoer relativt til `LocalDate.now()`, så regler for alder, frister og meldeperioder fortsetter å gjelde.
- Bruk `@ParameterizedTest` + `@EnumSource`/`@MethodSource`, `@TableTest` for tabellbaserte tilfeller, og
  `@ParameterizedClass @MethodSource("unleashTestDataSource")` for å kjøre en flyttest med begge fake Unleash-oppsettene (dobler
  kjøretiden, bruk bare når toggles endrer atferden).
- Maler for hver type: [references/maler.md](references/maler.md).

## Fallgruver (verifisert)

- In-memory repositories er for det meste Kotlin-`object`er: **tilstanden deles av alle tester i JVM-en**. Opprett ny
  sak/behandling per test (`opprettInMemorySakOgBehandling()`, ID-er fra 10000 og oppover), og ikke anta at repositoryet er tomt.
  Åtte er klasser (ny instans per `RepositoryProvider`); noen fake-metoder er `TODO()`, implementer dem i lib-test.
- Repository-tester deler én database per klasse (uten tømming): hver test oppretter sin egen sak/behandling.
- `Repository av typen 'X' er ikke registrert` / `Gateway av typen 'X' er ikke registrert` → registrer fake-en i
  `inMemoryRepositoryRegistry` / din `createGatewayProvider { }` (eller `testGatewayProvider()` + `FakeServers`).
- `feature toggle X ikke definert for fake` → legg til togglen i `LokalUnleash` (lib-test `FakeUnleash.kt`).
- Flyttest feiler på en uventet måte → `ManuellMotorImpl` fanget en jobbfeil (`Feil under prosessering av jobb`);
  `kjor_test.py --resultat` viser dem. Se mer i [references/flyttester.md](references/flyttester.md).
- `Could not find a valid Docker environment` → Docker/Colima kjører ikke, eller miljøvariabler mangler.
- `No tests found for given includes` → feil modul eller et filter som ikke treffer noen tester.
