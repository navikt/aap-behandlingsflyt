# aap-behandlingsflyt – instruksjoner for KI-agenter

Backend for saksbehandling av arbeidsavklaringspenger (Kelvin). Kotlin 2.4, Ktor, Postgres, Gradle.
En motor (`FlytOrkestrator`) driver hver behandling gjennom en fast liste av steg; mennesker svarer på
avklaringsbehov underveis. Les dette før du rører et steg, en løser, et informasjonskrav eller et repository.

## Språk og stil
- Norsk bokmål overalt: identifikatorer (med æøå), KDoc, loggmeldinger, testnavn (backtick-setninger),
  commit-meldinger. Ikke translitterer (`loes`), ikke bland inn engelsk.
- Kotlin official style, 4 mellomrom, 120 kolonner, ingen trailing comma, ingen star imports (`.editorconfig`).
  Det finnes ingen formatter i CI; følg filen du står i.
- Feilhåndtering: `UgyldigForespørselException` for brukerfeil (blir HTTP 400),
  `IllegalStateException("[Utviklerfeil] …")` for invarianter. Ikke innfør Result-typer eller nye unntakshierarkier.
- Alt er blokkerende IO uten coroutines. Transaksjoner åpnes kun i `api` (per kall) og av Motor (per jobb); domenet
  åpner aldri transaksjoner.

## Moduler
| Modul | Innhold |
|---|---|
| `app` | Oppstart. `App.kt` er komposisjonsroten: ruter, Kafka-konsumenter, `ProsesseringsJobber.alle()`. |
| `api` | Ktor-ruter (`*GrunnlagApi.kt` per steg, `FlytApi`, `AvklaringsbehovApi`, `DriftApi`) og DTO-er. Ingen logikk i rutefiler. |
| `behandlingsflyt` | Domene og motor: `flyt/` (motor), `forretningsflyt/` (flytdefinisjoner og `*Steg`), `behandling/avklaringsbehov/` (løsere, løsninger, `AvklaringsbehovService`), `faktagrunnlag/` (grunnlag, informasjonskrav, repository-grensesnitt), `prosessering/` (`*JobbUtfører`), `hendelse/`. |
| `repository` | `*RepositoryImpl` (håndskrevet SQL via dbconnect), `integrasjon/` (gateways), `src/main/resources/flyway/`. |
| `lib-test` | `InMemory*Repository`, `Fake*` HTTP-servere for alle eksterne systemer, `TestApp`, `TestScenarioOrkestrator`. |
| `kontrakt` | **Publiseres som bibliotek** til oppgave, tilgang, statistikk, postmottak. `Definisjon`, `StegType`, hendelse-DTO-er. |

Nye repositories legges i `repository/`, gateways i `repository/.../integrasjon/`, grensesnittet i domenet ved siden
av modellen. Ny funksjonalitet samles i en feature-pakke `no.nav.aap.<feature>` speilet i `api`, `behandlingsflyt`
og `repository` (konvensjon fra oktober 2026, se `no.nav.aap.meldeplikt`).

## Kommandoer
```bash
./gradlew :behandlingsflyt:compileKotlin                       # rask kompilering av én modul
./gradlew :behandlingsflyt:coreTest                            # alle kjernetester i modulen, uten Docker (~15 s)
./gradlew :behandlingsflyt:test --tests "no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderAlderStegTest"
./gradlew :repository:test --tests "*SykdomRepositoryImplTest" # DB-test, krever Docker (Testcontainers)
./gradlew :behandlingsflyt:check                               # som CI gjør for modulen
./gradlew :arkitektur:test                                     # strukturregler: registreringslister, lagdeling, aktiv-flagg, pekere i instruksjonsfilene (~10 s)
./gradlew runTestApp                                           # app på :8080 med fakes; token: curl -s -XPOST http://localhost:8081/token
./gradlew detekt                                               # feiler aldri bygget; ikke rydd baseline-funn i urelaterte PR-er
```
Tester som trenger Docker er tagget `@Tag("db")`/`@Tag("kafka")` (flyttester arver `db`+`flyt` fra
`AbstraktFlytOrkestratorTest`); `coreTest` kjører resten uten Docker. Ny test som bruker `TestDataSource`,
`DatabaseSnapshot`, `postgreSQLContainer()` eller Kafka skal ha tag, ellers feiler `coreTest` med beskjed om det.
Tidsbruk (målt): inkrementell kompilering ~1 s, endring + `:behandlingsflyt:coreTest` ~15 s, én DB-test ~10-15 s fast
kostnad (container + Flyway), `:behandlingsflyt:test` ~4-5 min, `./gradlew test` ~5 min. Kjør `coreTest` etter hver
endring, målrettede DB-/flyttester for det du har rørt, og hele `test` før du leverer. Ikke bruk `assemble`/`build` i
arbeidsløkka (pakker fat-jar hver gang). Feiler Testcontainers med "Could not connect to Ryuk" eller "Connection to
localhost:NNNN refused", er det Docker-miljøet, ikke koden: kjør på nytt (eventuelt med `TESTCONTAINERS_RYUK_DISABLED=true`).
Kjør git uten pager og editor (`git --no-pager log`, `git --no-pager diff`, `git merge --no-edit`), så kommandoen
ikke blir stående og vente i terminalen.

## Harde regler (brudd knekker prod eller historiske behandlinger)
1. **Aldri endre, slette eller omdøpe** verdier i `Definisjon`, `StegType`, `AvklaringsbehovKode`, `Vurderingsbehov`,
   `InformasjonskravNavn` eller `JobbUtfører.type`. Navnene ligger lagret i databasen og i publisert kontrakt.
   Legg til ny verdi og merk den gamle `@Deprecated`.
2. **Alt under `kontrakt/` publiseres automatisk** som ny versjon ved merge til main. Endringer der krever bump i
   aap-oppgave, aap-tilgang, aap-statistikk og aap-postmottak-backend før den nye verdien tas i bruk. Si fra før du rører `kontrakt/`.
3. **Stegrekkefølgen er semantikk.** Tilbakeføring og sortering bruker indeksene i `Revurdering.kt` m.fl. Å flytte,
   fjerne eller sette inn steg brekker åpne behandlinger (`BehandlingFlyt.steg()` kaster). Spør først.
4. **Grunnlag oppdateres aldri in-place.** Mønsteret er deaktiver (`aktiv = false`) + insert ny rad, per
   `behandling_id`. Utover den deaktiveringen er `UPDATE`/`DELETE` bare lov på sak/behandling/avklaringsbehov-hoder,
   låsversjon og i `slett()` (GDPR, hard sletting).
5. **Migrasjoner må tåle rullende deploy**: gammel kode kjører mot nytt skjema i flere minutter.
   `ADD COLUMN` nullable eller `NOT NULL DEFAULT`. Aldri `DROP`/`RENAME` i samme PR som koden som slutter å bruke det.
6. **Git**: aldri push til main, alltid PR, squash merge. Ikke commit eller push uten beskjed.

## Flytmodellen – det ikke-opplagte
- **En behandling er en snapshot-kjede.** Ny ytelsesbehandling får kopiert pekere til forrige behandlings aktive
  grunnlag (`GrunnlagKopierer` kaller `kopier` på alle registrerte repositories). "Gamle data" ligger derfor inne i
  den nye behandlingen, skilt fra nye bare ved `vurdertIBehandling` og `forrigeBehandlingId`.
- **Førstegangsbehandling og Revurdering kjører samme stegliste.** Oppførsel styres av `kontekst.vurderingType`
  og `vurderingsbehov`, aldri av `typeBehandling`.
- **Hvert steg kjøres mange ganger**: ved første pass, etter hvert svar fra saksbehandler, etter tilbakeføring,
  ved hvert `/forbered` etter 15 min inaktivitet, og hele flyten på nytt i hver automatiske revurdering
  (meldekort hver 14. dag, fritak, G-regulering, vedtakslengde). `utfør` må være en idempotent "avstem ønsket
  tilstand"-funksjon.
- **Avklaringsbehov avstemmes, de "kastes" ikke.** Bruk `AvklaringsbehovService.oppdaterAvklaringsbehov*` med
  ekte `erTilstrekkeligVurdert` og `tilbakestillGrunnlag`. Motoren leser bare repositoryet i `AVKLARINGSPUNKT`.
- **Tilbakeføring kjører ingen kode.** Den skriver bare `TILBAKEFØRT`-markører; opprydding skjer når steget kjører
  igjen. Glemt `tilbakestillGrunnlag` gir gamle vilkårsresultat i vedtaket.
- **Informasjonskrav styrer re-kjøring.** `erRelevant` er rate-limiter, `oppdater` som returnerer `ENDRET` tilbakefører
  behandlingen til ditt steg. `hentData` kjører på virtuell tråd uten DB-tilkobling; `klargjør`/`oppdater` kaller
  ikke eksterne tjenester.
- **Løsere gjør minst mulig**: valider, lagre `nye ∪ tidligere vedtatte` vurderinger, returner. Ikke reduser til
  tidslinje før lagring, ikke rør avklaringsbehov-status, ikke kjør vilkårslogikk.
- **`Status.erAvsluttet()` er sann for `IVERKSETTES`.** Bruk `erVedtatt()` for "har vedtak" og sjekk `== AVSLUTTET`
  for "kan ikke lenger endres".
- **Jobben er én transaksjon med savepoints ved steggrenser.** Et unntak i `utfør` ruller bare tilbake dette steget;
  resten committes, jobben prøves 3 ganger og blokkerer deretter behandlingen til drift griper inn. Atomære
  (meldekort-)behandlinger har ingen savepoints og må fullføre i ett.
- **Én åpen ytelsesbehandling per sak** er en serviceregel, ikke en DB-constraint. Nye hendelser på en åpen
  behandling legger bare til `vurderingsbehov`.
- **Periodisering er normalen.** Vilkår og vurderinger er tidslinjer over `rettighetsperiode`;
  bruk `PeriodisertAvklaringsbehovLøsning` og de periodiserte hjelperne.

## Registreringslister som må oppdateres for hånd
Disse kompilerer uten endring og feiler først i runtime (`"Repository av typen … er ikke registrert"`):
`repository/.../PostgresRepositoryRegistry.kt`, `repository/.../integrasjon/DefaultGatewayProvider.kt`,
`lib-test/.../TestGatewayProvider.kt`, `lib-test/.../inmemoryrepo/InMemoryRepositoryRegistry.kt`,
`prosessering/ProsesseringsJobber.kt`, `app/.../App.kt` (ruter), `lib-test/.../FakeUnleash.kt` (nye toggles).
`./gradlew :arkitektur:test` sjekker alle listene unntatt ruter, og sier hvilken fil som mangler hva. Kjente brudd
står i `kjenteBrudd` i testene; listene skal bare krympe, så ikke legg til nye der for å få testen grønn.
Alle injiserbare klasser følger samme idiom: `private constructor(avhengigheter)` +
`constructor(repositoryProvider, gatewayProvider)` + `companion object : FlytSteg | Factory | Informasjonskravkonstruktør | ProvidersJobbSpesifikasjon`.
Parametertyper må være marker-grensesnittet, aldri `*Impl`. Nytt repository må implementere `kopier` og `slett`.

Regler for enkeltområder (steg, avklaringsbehov, informasjonskrav, repository, migrasjoner, kontrakt, api) ligger i
`.github/instructions/` og gjelder når du lager eller endrer filer i området.
Oppskrifter for vanlige endringer finnes som skills i `.github/skills/`: `nytt-steg`, `nytt-avklaringsbehov`,
`nytt-informasjonskrav`, `nytt-grunnlag`, `ny-jobb`. Bruk dem.
Til gjennomgang av en diff: skillen `flyt-review` (sjekkliste) og agenten `flyt-reviewer` i `.github/agents/`.

## Testing
- Nivåer: ren enhetstest (vilkår, tidslinjer), domenetest med `InMemory*`-repos, DB-test mot Postgres
  (`TestDataSource`), flyttest (`*FlytOrkestratorTest`, `KlageFlytTest` – asserterer stegsekvens), E2E i `app`
  (`OpprettOgFullførBehandlingApiTest` med `@Fakes`).
- Ny løser → `*LøserTest`. Nytt repository → `*RepositoryImplTest` mot Postgres **og** `InMemory*`-variant.
  Nytt steg → `*StegTest` og oppdater flytsekvenstestene. Ny toggle → `FakeUnleash`; tester kjører med alle
  toggles av, så ny oppførsel bak toggle er utestet til testen skrur den på.
- Fakes framfor mocks. Ikke "fiks" en test ved å endre forventningen uten å forstå hvorfor den feilet.
- Før du sier "ferdig": kjør testene i modulene du endret (skillen `verifiser` velger dem ut fra endringene).
  Har du rørt `forretningsflyt/`, `flyt/` eller `avklaringsbehov/`, kjør også
  `:behandlingsflyt:test --tests "*FlytOrkestratorTest" --tests "*FlytTest"`.

## Naborepoer
Les kildekoden i naborepoet i stedet for å pakke ut jar-filer. Finnes ikke mappen, si fra i stedet for å lete i
Gradle-cachen. Endringer i et naborepo er en egen PR der; ikke skriv der uten at mennesket ber om det.
- `../aap-postmottak-backend`: tar imot journalposter og sender dem hit som hendelser fra `kontrakt/…/hendelse/`
  (`BehandlingsflytGateway.sendHendelse`, `OverleverTilFagsystemSteg`). Les der for å se hva som sendes og når.
- `../aap-intern-sysdok`: dokumentasjonen nedenfor.

## Dokumentasjon
- `../aap-intern-sysdok/docs/funksjonalitet/06_Behandlingsflyt/` – `best_practices.md` og `utviklerguide.md` gjelder.
  Tilstandsmaskinen i `teknisk.md` er utdatert (mai 2026).
- Den gjeldende tilstandsmaskinen er koden: `flyt/steg/Transisjon.kt`, `StegOrkestrator.kt` og `FlytOrkestrator.kt`.
- Generert oversikt over alle avklaringsbehov og steg: https://navikt.github.io/aap-behandlingsflyt/
- Testendepunkter i `api/.../test/` er bevisst med i `main` (skrudd av i prod). Ikke slett dem, ikke legg prodkode der.
