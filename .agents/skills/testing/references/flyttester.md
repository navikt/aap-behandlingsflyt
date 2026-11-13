# Flyttester (AbstraktFlytOrkestratorTest)

Helhetstester av motoren: ekte Postgres, alle repositories, alle gateways mot HTTP-fake-ene og motorjobbene,
styrt gjennom en liten DSL. Dette er den tregeste testtypen (4–70 s per klasse på en rask bærbar): kjør maksimalt ~3 klasser /
~20 tester per Gradle-kjøring.

## Hva basisklassen setter opp (`behandlingsflyt/src/test/.../flyt/AbstraktFlytOrkestratorTest.kt`)

- `@Fakes` → `FakeServers.start()` én gang per JVM. Før hver test nullstilles `FakePersoner`, statistikkhendelser,
  legeerklæringsstatuser og journalposter i Dokarkiv.
- `dataSource = TestDataSource()` per testklasse (`@BeforeAll`): en ny database klonet fra en mal som Flyway
  migrerte én gang per JVM i en Postgres 18 Testcontainer. Før hver test **tømmes alle tabeller**.
- `motor = ManuellMotorImpl(dataSource, ProsesseringsJobber.alle(), postgresRepositoryRegistry, gatewayProvider)`;
  `gatewayProvider = testGatewayProvider(<Unleash class from the constructor>)` (real gateway impls → `FakeServers`).
- Hjelpere som endrer tilstand, avslutter med `motor.kjørJobber()` (kjører alle forfalte jobber synkront, inkludert
  `flyt.prosesserBehandling` som de utløser) og returnerer en **ny** `Behandling` lest fra databasen. Kjed alltid videre
  fra returverdien. En lagret `behandling`-variabel blir utdatert (status, aktivt steg, versjon).

Vis hjelperne med signaturer i stedet for å lese fila på 1 700 linjer:
`python3 .agents/skills/testing/scripts/kjor_test.py --dsl` (filter: `--dsl løs`, `--dsl Sak.`, `--dsl assert`).

## Eksempel

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

`happyCaseFørstegangsbehandling()` kjører en hel førstegangsbehandling og returnerer `Sak`;
`revurdereFramTilOgMedSykdom(sak, gjelderFra)` starter en revurdering og løser sykdom.

## Oversikt over DSL-en

- Inndata: `sendInnFørsteSøknad(søknad, person, mottattTidspunkt)`, `Sak.sendInnSøknad/sendInnMeldekort/sendInnKlage/
  sendInnKabalHendelse/sendInnOppfølgingsoppgave`, `Sak.opprettManuellRevurdering(vurderingsbehov)`,
  `Behandling.leggTilVurderingsbehov(...)`, `settBehandlingPåVent`, `Behandling.bestillLegeerklæring()`.
- Løse behov: generisk `Behandling.løsAvklaringsBehov(løsning, bruker = Bruker("SAKSBEHANDLER"))` og hjelperne `løsSykdom`,
  `løsBistand`, `løsLovvalg`, `løsForutgåendeMedlemskap`, `løsYrkesskade`, `løsBeregningstidspunkt`,
  `løsFastsettManuellInntekt`, `løsSamordning*`, `løsBarnetillegg`, `løsRettighetsperiode`, `løsVedtakslengde`,
  `bekreftVurderinger`, `kvalitetssikre(...)` (Bruker "KVALITETSSIKRER"), `fattVedtak()` /
  `beslutterGodkjennerIkke(...)` (Bruker "BESLUTTER"), `løsVedtaksbrev()`; snarvei `løsFramTilGrunnlag`.
- Kontrollere: `medKontekst { avklaringsbehovSomMåLøses; ventebehov; behandling; avklaringsbehovene; repositoryProvider }`
  (ferske data i en transaksjon), `hentStegHistorikk`, `hentVilkårsresultat`, `hentVedtak`, `hentBrevAvType`,
  `assertVilkårsutfall`, `assertRettighetstype`, `assertUnderveis`, `hentSisteOpprettedeBehandlingForSak`,
  `Behandling.assertThrows(...)`. Kjør motoren direkte på nytt: `prosesserBehandling(behandling)`.
- Personer: `TestPersoner.STANDARD_PERSON()`, `PERSON_MED_YRKESSKADE()`, `PERSON_FOR_UNG()`, `PERSON_61()`,
  `PERSON_62()`, `PERSON_MED_FORUTGÅENDE_MEDLEMSKAP()` eller `nyPerson(...)`. Kallene registrerer personen i
  `FakePersoner`, som HTTP-fake-ene (PDL, inntekt, yrkesskade, …) leser fra. Søknader: `TestSøknader.STANDARD_SØKNAD`,
  `SØKNAD_STUDENT`, `SØKNAD_YRKESSKADE`, `SØKNAD_INGEN_MEDLEMSKAP`, `SØKNAD_MED_BARN(listOf(navn to fødselsdato))`.
- Unleash: `AlleAvskruddUnleash` (alt av, bortsett fra `IngenValidering`, som i produksjon), `LokalUnleash` (lokale standardverdier; må
  liste opp alle toggles), eller et testlokalt `object X : FakeUnleashBaseWithDefaultDisabled(listOf(...))`.
- Et nytt avklaringsbehov trenger en hjelper her: `protected fun Behandling.løsNyVurdering(...): Behandling =
  løsAvklaringsBehov(NyVurderingLøsning(...))`.

## Snapshot-tester (`AbstraktFlytOrkestratorSnapshotTest`)

For mange tester som starter fra samme ferdigstilte tilstand (vanligvis en vedtatt førstegangsbehandling): oppsettet kjøres
én gang, databasen fryses som en Postgres-mal, og hver test får sin egen klone.

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

Eksempler: `KvalitetssikringFlytTest`, `OppfølgingsBehandlingFlytTest`, `AktivitetspliktFlytTest`,
`overgangarbeid/OvergangArbeidFlytTest`.

## Kjøre tester

- `python3 .agents/skills/testing/scripts/kjor_test.py --liste flyt` viser alle 34 klassene og antall tester
  (`5x2` = `@ParameterizedClass`; hver test kjøres én gang per Unleash-oppsett).
- Kjør klassen som dekker endringen, og eventuelt én nærliggende klasse. Kjør alle bare ved større endringer (rekkefølge på flyt,
  motor, delte hjelpere): `kjor_test.py --batch flyt` og deretter `--run 1`, `--run 2`, … én per verktøykall.
- Én metode: `kjor_test.py VedtakslengdeFlytTest -m "del av navnet"`.

## Fallgruver (verifisert)

- **Jobbfeil får ikke testen til å feile direkte.** `ManuellMotorImpl` fanger unntaket, logger `Feil under prosessering av jobb`,
  markerer jobben (prøver på nytt opptil 3 ganger, deretter FEILET), og testen feiler senere med en forvirrende påstand.
  Jobben kjører også på forbindelsen som henter den, så skrivingene fra den feilede jobben **rulles ikke tilbake** (i produksjon rulles de tilbake til
  siste savepoint). Se etter logglinjen først: `kjor_test.py --resultat behandlingsflyt` viser dem. Noen tester som består,
  logger også en slik feil (`TrekkOgAvbrytFlytTest` og `KvoteOgVarighetFlytTest` gjorde det da dette ble skrevet), så
  sammenlign med samme test før endringen før du legger skylden på den.
- Jobber med `neste_kjoring` i framtiden (for eksempel `flyt.informasjonskrav` som er planlagt senere) og cron-jobber kjøres ikke i
  tester med mindre du utløser dem selv.
- Informasjonskrav begrenses per sak (ofte til én gang om dagen). For å hente data på nytt i samme test, kall
  `nullstillInformasjonskravOppdatert(InformasjonskravNavn.X, sak.id)` etter at du har endret fake-dataene.
- `IngenValidering` er aktivert i begge standardoppsettene for fake Unleash, så fireøyneprinsippet
  (`KanIkkeVurdereEgneVurderingerException`) testes ikke.
- Ny `BehandlingsflytFeature` → legg den til i `LokalUnleash`, ellers feiler alle `LokalUnleash`-tester med
  `feature toggle X ikke definert for fake`.
- Ny gateway → registrer den i `testGatewayProvider()` og legg til en HTTP-fake i `FakeServers` (se fakes.md).
- `FakeServers`/`FakePersoner` er singleton-objekter globale for JVM-en. Testene kjører sekvensielt i samme JVM, så nullstilling i
  `@Fakes` er nok. Ikke behold persondata i statiske felter mellom tester.
- Datoer er relative til `LocalDate.now()`. Ikke hardkod datoer som blir for gamle for regler om alder, frister eller meldeperioder.
