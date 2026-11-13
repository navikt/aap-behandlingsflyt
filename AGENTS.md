# AGENTS.md — aap-behandlingsflyt

Backend for Kelvin, Navs saksbehandlingssystem for arbeidsavklaringspenger (AAP). Det definerer *behandlingsflyten*,
som er rekkefølgen sakene går gjennom, driver hver behandling gjennom stegene med en databasebasert tilstandsmaskin
som kjøres av bakgrunnsjobber, og tilbyr REST-API-er til saksbehandlingsløsningen. Kotlin 2.4 / JVM 25, Ktor, PostgreSQL +
Flyway og Navs `no.nav.aap.kelvin:*`-biblioteker «komponenter» (`motor`, `dbconnect`, `tidslinje`, `httpklient`, …).
Kode og domenespråk er norsk (identifikatorer beholder æ/ø/å), og dokumentasjonen er på norsk.

## Moduler

| Modul | Innhold |
|---|---|
| `kontrakt` | Bibliotek som publiseres til andre Kelvin-apper: `StegType`, `StegGruppe`, `Definisjon`/`AvklaringsbehovKode`, `Status` for behandling/sak, `TypeBehandling` og event-DTO-er. Endringer påvirker aap-tilgang, aap-oppgave, aap-statistikk og frontend. Disse må oppgradere versjonen. |
| `behandlingsflyt` | Domene og motor. Repository- og gateway-**grensesnittene** ligger her. Pakkeroten er `no.nav.aap.behandlingsflyt`; nyere vertikale feature-pakker er `no.nav.aap.{bistandsbehov,meldeplikt,overgangarbeid}`. |
| `repository` | Postgres-implementasjoner av repository, Flyway-migreringer (`src/main/resources/flyway`), HTTP-gatewayer (`integrasjon/`), `postgresRepositoryRegistry`, `defaultGatewayProvider()` |
| `api` | Ktor-ruter (`*Api.kt`), respons-DTO-er og Kafka-konsumenter (`hendelse/kafka`) |
| `app` | Kobling i `App.kt` (datakilder, Motor, ruter, Kafka), backfill-jobber og tester, inkludert `TestApp` |
| `lib-test` | Testoppsett: `FakeServers` (HTTP-fakes), `FakePersoner`, fake Unleash, in-memory-repository og `testGatewayProvider()` |
| `docs`, `build-logic` | Dokka-nettsted og `docs/playbook.md` (prosedyrer for hendelser); Gradle-konvensjonsplugin `aap.conventions` |

Viktige stier under `behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/`: `flyt/` (motor), `forretningsflyt/behandlingstyper/`
(flytdefinisjoner; `Revurdering.kt` er selve flyten, `Førstegangsbehandling` bruker den på nytt), `forretningsflyt/steg/` (steg),
`behandling/avklaringsbehov/` (+ `løsning/`, `løser/`), `behandling/vilkår/`, `faktagrunnlag/` (informasjonskrav, grunnlag),
`prosessering/` (jobber, `ProsesseringsJobber.alle()`), `hendelse/mottak|avløp/` (inn- og utgående hendelser), `sakogbehandling/`
(Sak, Behandling, BehandlingService, Vurderingsbehov, låser), `periodisering/` (VurderingType per steg).

## Bygging og testing (detaljer: `testing`)

- Bruk alltid wrapperen `./gradlew` fra repoets rot, aldri en globalt installert `gradle`.
- **Kjør aldri hele testsettet** (`./gradlew test`/`check`/`build` eller en ufiltrert `:behandlingsflyt:test`,
  `:repository:test`, `:api:test`, `:app:test`). Det tar lang tid og får tidsavbrudd i agent-sandkasser. Kjør én klasse eller en liten
  gruppe per kjøring med `--tests`, én kjøring per verktøykall. Kjør maksimalt rundt tre flyttester og `app`-integrasjonstester
  per runde.
  `python3 .agents/skills/testing/scripts/kjor_test.py <Klasse>` skriver ut kommandoen; `--batch flyt` deler opp større grupper.
- Eksempel: `./gradlew :behandlingsflyt:test --tests 'no.nav.aap.behandlingsflyt.flyt.AlderFlytTest'`. Kompiler uten
  å kjøre tester med `./gradlew compileTestKotlin`.
- Krever JDK (verktøykjede JDK 25) og Docker for Testcontainers (Colima: miljøvariabler i README). Sjekk med `kjor_test.py --sjekk-miljo`.
- CI kjører `./gradlew :<module>:check` for hver modul (api, app, behandlingsflyt, kontrakt, lib-test, repository).
- `./gradlew detekt` (gir bare rapport). Lokal app med fakes: `./gradlew runTestApp` → http://localhost:8080/swagger-ui/index.html.
- Hvis du ikke fikk kompilert eller kjørt tester, si det tydelig. Ikke påstå at de passerer.

## Slik går en behandling gjennom flyten (detaljer: `flyt-orkestrering`)

1. Dokument/hendelse inn → jobben `hendelse.håndterer` → `BehandlingService.finnEllerOpprettBehandling` oppretter en behandling eller legger til
   *vurderingsbehov* i den åpne behandlingen → legger jobben `flyt.prosesserBehandling` i kø.
2. Jobben kjører `FlytOrkestrator.forberedOgProsesserBehandling`: *forbered* (løs ventebehov, oppdater informasjonskrav,
   eventuelt gå tilbake) og deretter *prosesser* (steg i flytrekkefølge; hvert steg: START → OPPDATER_FAKTAGRUNNLAG → UTFØRER → AVKLARINGSPUNKT → AVSLUTTER).
3. Flyten stopper ved det første steget med et åpent avklaringsbehov som skal løses der, eller lukker behandlingen når den er ferdig.
4. Saksbehandleren løser behovet via `POST /api/behandling/løs-behov` → løseren lagrer vurderingen, behovet får status AVSLUTTET, og en ny
   `flyt.prosesserBehandling`-jobb legges i kø (HTTP 202). Flyten fortsetter asynkront.

## Regler det er lett å misforstå (alle er kontrollert mot koden)

1. Et stegs `utfør` kjøres på nytt hver gang flyten passerer steget eller fortsetter derfra (fra START). Sørg for at det er idempotent, og avstem
   avklaringsbehov gjennom `AvklaringsbehovService`, aldri manuelt.
2. Stegene stopper ikke flyten, det gjør avklaringsbehov (kontrolleres i AVKLARINGSPUNKT). Alle steg i en flyt kjøres alltid;
   `vurderingsbehovRelevanteForSteg` filtrerer bare konteksten og styrer tilbakeføring.
3. Tilbakeføring skriver bare TILBAKEFØRT i steghistorikken. Ingen stegkode kjøres, og ingen data rulles tilbake.
4. `Status.erAvsluttet()` er også sann for IVERKSETTES (behandling) og AVBRUTT (avklaringsbehov).
5. `Førstegangsbehandling.flyt()` == `Revurdering.flyt()`. Bruk `BehandlingService.utledFaktiskBehandlingstype()` for å finne den faktiske behandlingstypen.
6. `behandling.flyt()` oppretter en ny `BehandlingFlyt` på det FØRSTE steget. Kall `forberedFlyt(behandling.aktivtSteg())` først.
7. En jobb som feiler i `flyt.prosesserBehandling`, lagrer arbeidet fram til siste savepoint (etter START/OPPDATER_FAKTAGRUNNLAG i hvert
   steg). Den ruller ikke tilbake alt. Jobber kjøres minst én gang, så gjør dem idempotente.
8. Motor serialiserer jobber bare per (sak_id, behandling_id, type). En KLAR/FEILET `flyt.prosesserBehandling` blokkerer nye utløsere og
   saksbehandling (`BehandlingUnderProsesseringException`). Standardinnstillingen er tre umiddelbare forsøk, deretter FEILET fram til en retry-cron.
9. `Informasjonskrav.hentData` kjører i parallelle tråder: ingen DB der. `klargjør`/`oppdater`: ingen HTTP.
10. Lagrede enum-navn (StegType, Definisjon, Vurderingsbehov, InformasjonskravNavn, ÅrsakTilOpprettelse, jobbenes `type`
    osv.) må aldri få nytt navn eller fjernes. Deprecated dem i stedet. Endringer i stegrekkefølge eller fjerning av steg kan ødelegge åpne behandlinger.
11. Revurdering kopierer grunnlag grunt (`Repository.kopier`: ny grunnlagsrad → delte underordnede rader). Oppdater aldri underordnede rader.
    Avklaringsbehov, vedtak, brev og logger kopieres IKKE (`behandlingsflyt/behandlingsflyt.md` sier noe annet, men det er feil).
12. Når du legger til `BehandlingsflytFeature`, legg den også til i `LokalUnleash` (lib-test). Den kaster feil for ukjente toggler.
13. I tester skjuler `ManuellMotorImpl.kjørJobber()` jobbfeil («Feil under prosessering av jobb»). Sjekk loggen hvis en
    flyttest feiler på en uventet måte.
14. JVM og DB-sesjonens tidssone må være Europe/Oslo. `Periode` inkluderer begge endepunktene. `Tid.MAKS` = 2999-01-01.

## Konvensjoner

- Kobling uten DI-rammeverk: primærkonstruktøren tar avhengigheter (kan testes), sekundærkonstruktøren
  `constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider)` oppretter dem med
  `repositoryProvider.provide()` / `gatewayProvider.provide()`.
- Steg: `class X : BehandlingSteg` + `companion object : FlytSteg { konstruer(); type() }`; flytene viser til companion-objektet.
  Informasjonskrav: `companion object : Informasjonskravkonstruktør`. Jobber: `companion object : ProvidersJobbSpesifikasjon`,
  registrert i `ProsesseringsJobber.alle()`. Repositories: `companion object : Factory<…>`, registrert i
  `PostgresRepositoryRegistry.kt`. Gatewayer registreres i `defaultGatewayProvider()` og `testGatewayProvider()`.
- Steg bruker en uttømmende `when` uten `else` for `kontekst.vurderingType`. Bruk `TidligereVurderinger` til å hoppe over arbeid
  når et tidligere steg allerede gir avslag.
- Periodiserte data bruker `Tidslinje`/`Periode` fra komponenter (`map2`/`outerJoin` er outer joins).
- Behold `public`-modifikatorer i `kontrakt` (explicit API-modus). Flyway-migreringer kan ikke endres etter merge.
- Følg stilen rundt koden. Skriv kommentarer bare når oppførselen ikke er åpenbar.
- Navngi boolske hjelpemetoder etter det de faktisk sjekker (for eksempel `harIkkeMigrertKrav`, ikke `harIkkeVurderteMigrerteKrav`).
- Alle commit-meldinger starter med en tagg i hakeparenteser som knytter arbeidet til et mål, for eksempel `[MIGRERING] …`. Teamet bruker
  et fast sett med bestemte tagger. Aldri finn på eller gjett en tagg, heller ikke for en separat eller urelatert commit. Spør alltid
  brukeren hvilken tagg som skal brukes for hver commit, og vis hele commit-meldingen for godkjenning før du committer. Commit-
  meldinger skrives alltid på norsk bokmål.

## Hvor du finner ting

| Behov | Se her |
|---|---|
| Steg i en flyt, steg ↔ klasse, hvem som reagerer på et vurderingsbehov | `flyt.py --flyt Revurdering`, `--steg X`, `--vurderingsbehov Y`; generert `.agents/skills/flyt-orkestrering/references/flyter.md` |
| Koder for avklaringsbehov, steg, roller, løsning/løser | `avklaringsbehov.py --kode 5003`; generert `.agents/skills/avklaringsbehov/references/definisjoner.md` |
| Jobbtyper | `jobber.py`; `prosessering/ProsesseringsJobber.kt` |
| Feature-toggles | `behandlingsflyt/.../unleash/FeatureToggle.kt` (`BehandlingsflytFeature`) |
| API-ruter | `app/.../App.kt` (`routing { apiRouting { … } }`), implementasjoner i `api/` |
| Miljøkonfigurasjon | `.nais/dev.yaml`, `.nais/prod.yaml` |
| Kildekode for motor/dbconnect/tidslinje | Finnes ikke i dette repoet: github.com/navikt/aap-kelvin-komponenter, versjon `komponenter` i `gradle/libs.versions.toml` |
| Prosedyrer ved hendelser | `docs/playbook.md` |

## Grunnleggende ordliste (mer i `aap-domene`)

sak = en persons AAP-sak · behandling = én behandling av en sak · steg = trinn · flyt = steg i rekkefølge · avklaringsbehov =
beslutnings- eller ventepunkt for saksbehandleren (stopper flyten) · løsning/løser = innsendt beslutning / kode som lagrer den · vurdering = faglig vurdering ·
vurderingsbehov = grunn til å (vurdere på nytt), styrer VurderingType og tilbakeføring · informasjonskrav = innhenting fra register/dokument ·
grunnlag = lagrede fakta per behandling · vilkår = lovbestemt krav · tilbakeføring = å gå tilbake i flyten · beslutter/totrinn = godkjenning av en annen person ·
kvalitetssikring = kvalitetskontroll · NAY = nasjonal enhet (SAKSBEHANDLER_NASJONAL) · Nav-kontor = lokalkontor ·
iverksette = gjennomføre vedtaket · motor = jobbmotor-bibliotek · forbrenningskammer = motorens arbeidstråd.

## Hold dokumentasjonen oppdatert

Etter endringer i flyter, steg, Definisjon eller jobber må du oppdatere de genererte referansene:
`python3 .agents/skills/flyt-orkestrering/scripts/flyt.py --check .agents/skills/flyt-orkestrering/references/flyter.md`
og `python3 .agents/skills/avklaringsbehov/scripts/avklaringsbehov.py --check .agents/skills/avklaringsbehov/references/definisjoner.md`
(generer dem på nytt med `--markdown > <file>` hvis de er utdaterte). Oppdater en skill hvis du oppdager feil. Koden er fasiten.

Oppdater alltid disse agentinstruksjonene (denne fila eller den relevante skillen) når en agent har glemt noe, eller brukeren har
måttet be om en rettelse. Da unngår vi samme feil igjen.
