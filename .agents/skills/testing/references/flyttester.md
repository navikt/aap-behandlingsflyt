# Flyt-tests (AbstraktFlytOrkestratorTest)

End-to-end tests of the engine: real Postgres, every repository, every gateway against the HTTP fakes, and the motor
jobs, driven through a small DSL. Slowest kind of test (4–70 s per class on a fast laptop): run at most ~3 classes /
~20 tests per Gradle invocation.

## What the base class sets up (`behandlingsflyt/src/test/.../flyt/AbstraktFlytOrkestratorTest.kt`)

- `@Fakes` → `FakeServers.start()` once per JVM; before each test `FakePersoner`, statistikk events, legeerklæring
  statuses and Dokarkiv journalposter are reset.
- `dataSource = TestDataSource()` per test class (`@BeforeAll`): a fresh database cloned from a template that Flyway
  migrated once per JVM in a Postgres 18 Testcontainer. Before each test **all tables are truncated**.
- `motor = ManuellMotorImpl(dataSource, ProsesseringsJobber.alle(), postgresRepositoryRegistry, gatewayProvider)`;
  `gatewayProvider = testGatewayProvider(<Unleash class from the constructor>)` (real gateway impls → `FakeServers`).
- Every state-changing helper ends with `motor.kjørJobber()` (runs all due jobs synchronously, including the
  `flyt.prosesserBehandling` they trigger) and returns a **fresh** `Behandling` read from the database. Always chain on
  the returned value; a saved `behandling` variable goes stale (status, aktivt steg, versjon).

List the helpers with signatures instead of reading the 1 700-line file:
`python3 .agents/skills/testing/scripts/kjor_test.py --dsl` (filter: `--dsl løs`, `--dsl Sak.`, `--dsl assert`).

## Example

```kotlin
class MinFlytTest : AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {
    @Test
    fun `stopper på sykdom og innvilger`() {
        val (sak, behandling) = sendInnFørsteSøknad(person = TestPersoner.STANDARD_PERSON())
        behandling
            .medKontekst {
                assertThat(avklaringsbehovSomMåLøses.map { it.definisjon }).contains(Definisjon.AVKLAR_SYKDOM)
            }
            .løsFramTilGrunnlag(sak.rettighetsperiode.fom)   // sykdom, bistand, refusjon, brev, bekreft, kvalitetssikre
            .løsBeregningstidspunkt()
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsForeslåVedtak()
            .fattVedtak()
            .løsVedtaksbrev()
            .medKontekst { assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET) }
    }
}
```

`happyCaseFørstegangsbehandling()` runs a complete førstegangsbehandling and returns the `Sak`;
`revurdereFramTilOgMedSykdom(sak, gjelderFra)` starts a revurdering and solves sykdom.

## DSL overview

- Input: `sendInnFørsteSøknad(søknad, person, mottattTidspunkt)`, `Sak.sendInnSøknad/sendInnMeldekort/sendInnKlage/
  sendInnKabalHendelse/sendInnOppfølgingsoppgave`, `Sak.opprettManuellRevurdering(vurderingsbehov)`,
  `Behandling.leggTilVurderingsbehov(...)`, `settBehandlingPåVent`, `Behandling.bestillLegeerklæring()`.
- Solving: generic `Behandling.løsAvklaringsBehov(løsning, bruker = Bruker("SAKSBEHANDLER"))` and wrappers `løsSykdom`,
  `løsBistand`, `løsLovvalg`, `løsForutgåendeMedlemskap`, `løsYrkesskade`, `løsBeregningstidspunkt`,
  `løsFastsettManuellInntekt`, `løsSamordning*`, `løsBarnetillegg`, `løsRettighetsperiode`, `løsVedtakslengde`,
  `bekreftVurderinger`, `kvalitetssikre(...)` (Bruker "KVALITETSSIKRER"), `fattVedtak()` /
  `beslutterGodkjennerIkke(...)` (Bruker "BESLUTTER"), `løsVedtaksbrev()`; shortcut `løsFramTilGrunnlag`.
- Inspecting: `medKontekst { avklaringsbehovSomMåLøses; ventebehov; behandling; avklaringsbehovene; repositoryProvider }`
  (fresh data, inside a transaction), `hentStegHistorikk`, `hentVilkårsresultat`, `hentVedtak`, `hentBrevAvType`,
  `assertVilkårsutfall`, `assertRettighetstype`, `assertUnderveis`, `hentSisteOpprettedeBehandlingForSak`,
  `Behandling.assertThrows(...)`. Re-run the engine directly: `prosesserBehandling(behandling)`.
- Persons: `TestPersoner.STANDARD_PERSON()`, `PERSON_MED_YRKESSKADE()`, `PERSON_FOR_UNG()`, `PERSON_61()`,
  `PERSON_62()`, `PERSON_MED_FORUTGÅENDE_MEDLEMSKAP()`, or `nyPerson(...)`. Calling them registers the person in
  `FakePersoner`, which the HTTP fakes (PDL, inntekt, yrkesskade, …) read. Søknader: `TestSøknader.STANDARD_SØKNAD`,
  `SØKNAD_STUDENT`, `SØKNAD_YRKESSKADE`, `SØKNAD_INGEN_MEDLEMSKAP`, `SØKNAD_MED_BARN(listOf(navn to fødselsdato))`.
- Unleash: `AlleAvskruddUnleash` (all off except `IngenValidering`, prod-like), `LokalUnleash` (local defaults; must
  list every toggle), or a test-local `object X : FakeUnleashBaseWithDefaultDisabled(listOf(...))`.
- A new avklaringsbehov needs a helper here: `protected fun Behandling.løsNyVurdering(...): Behandling =
  løsAvklaringsBehov(NyVurderingLøsning(...))`.

## Snapshot tests (`AbstraktFlytOrkestratorSnapshotTest`)

For many tests that start from the same finished state (typically a vedtatt førstegangsbehandling): the setup runs
once, the database is frozen as a Postgres template, and every test gets its own clone.

```kotlin
class MinRevurderingTest : AbstraktFlytOrkestratorSnapshotTest(AlleAvskruddUnleash::class) {  // PER_CLASS lifecycle
    lateinit var sak: Sak

    @BeforeAll
    fun settOpp() = snapshotEtterSetup {
        val person = TestPersoner.STANDARD_PERSON()
        val (s, behandling) = sendInnFørsteSøknad(person = person)
        behandling.løsSykdom(s.rettighetsperiode.fom) /* … up to løsVedtaksbrev() … */
        sak = s
        this.person = person                 // re-registered in FakePersoner before every test
    }

    @Test
    fun `revurdering 1`() { sak.opprettManuellRevurdering(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND) /* … */ }
}
```

Examples: `KvalitetssikringFlytTest`, `OppfølgingsBehandlingFlytTest`, `AktivitetspliktFlytTest`,
`overgangarbeid/OvergangArbeidFlytTest`.

## Running

- `python3 .agents/skills/testing/scripts/kjor_test.py --liste flyt` lists all 34 classes with test counts
  (`5x2` = `@ParameterizedClass`, every test runs once per Unleash setup).
- Run the class that covers your change, then maybe one neighbour. All of them only when the change is broad (flow
  order, engine, shared helpers): `kjor_test.py --batch flyt` and then `--run 1`, `--run 2`, … one per tool call.
- A single method: `kjor_test.py VedtakslengdeFlytTest -m "del av navnet"`.

## Pitfalls (verified)

- **Failing jobs don't fail the test.** `ManuellMotorImpl` catches the exception, logs `Feil under prosessering av jobb`,
  marks the job (retried up to 3 times, then FEILET) and the test continues to fail later with a confusing assertion.
  It also runs the job on the picking connection, so the failed job's writes are **not rolled back** (prod rolls back to
  the last savepoint). Look for the log line first: `kjor_test.py --resultat behandlingsflyt` lists them. Some passing
  tests log one too (`TrekkOgAvbrytFlytTest` and `KvoteOgVarighetFlytTest` did when this was written), so
  compare with the same test before your change before blaming it.
- Jobs with `neste_kjoring` in the future (e.g. `flyt.informasjonskrav` planned for later) and cron jobs never run in
  tests unless you trigger them yourself.
- Informasjonskrav are throttled per sak (often once a day). To make a second fetch happen in one test call
  `nullstillInformasjonskravOppdatert(InformasjonskravNavn.X, sak.id)` after changing the fake data.
- `IngenValidering` is on in both standard fake Unleash setups, so the four-eyes rule
  (`KanIkkeVurdereEgneVurderingerException`) is not exercised.
- New `BehandlingsflytFeature` → add it to `LokalUnleash`, or every `LokalUnleash` test throws
  `feature toggle X ikke definert for fake`.
- New gateway → register it in `testGatewayProvider()` and add an HTTP fake in `FakeServers` (see fakes.md).
- `FakeServers`/`FakePersoner` are JVM-global singletons; tests in one JVM run sequentially, so the resets in
  `@Fakes` are enough — don't keep person data in static fields across tests.
- Dates are relative to `LocalDate.now()`; never hard-code dates that age out of rules (alder, frister, meldeperioder).
