---
name: motor-jobber
description: Motorens jobbmotor (no.nav.aap.motor fra aap-kelvin-komponenter), slik den brukes i behandlingsflyt: JobbUtfører/JobbSpesifikasjon, JobbInput (forSak/forBehandling, payload, prioritet), eksklusivitetsgrupper, scheduler og kjorbar, modellen med to transaksjoner og delvise commits ved savepoint, retry/backoff/FEILET-blokkering, cron-jobber og ManuellMotorImpl i tester. Bruk når du oppretter eller endrer en jobb, når en jobb feiler eller en behandling står «under prosessering», eller når du vurderer rekkefølge, samtidighet eller idempotens. Nøkkelord — motor, jobb, JobbUtfører, FlytJobbRepository, forbrenningskammer, cron, retry, kjorbar, flyt.prosesserBehandling.
---

# Motor (jobbmotor)

Biblioteket finnes **ikke i dette repoet**: `no.nav.aap.kelvin:motor` (+ `motor-api`, `motor-test-utils`), versjon =
`komponenter` i `gradle/libs.versions.toml`. Kilde: https://github.com/navikt/aap-kelvin-komponenter (`motor/`).
Les [references/internals.md](references/internals.md) for nøyaktig SQL og interne detaljer. Bruk feilsok-behandling-skillen
for å undersøke en jobb som feiler i et miljø.

Oversikt over appens jobber (type-streng, klasse, cron, retry og gruppering):
`python3 .agents/skills/motor-jobber/scripts/jobber.py` · `jobber.py --type flyt.prosesserBehandling`

## 1. Koblinger i denne appen

`App.kt` → `startMotor`: 4 workers («forbrenningskammer», `AppConfig.ANTALL_WORKERS_FOR_MOTOR`) per pod, egen Hikari-pool
«motor» (2 tilkoblinger per worker), `enableV2 = { true }` (scheduler + `kjorbar`-kolonne), jobber fra
`prosessering/ProsesseringsJobber.alle()`, `RetryService.enable()` ved oppstart, starter ved `ApplicationStarted` og stopper ved
`ApplicationStopping`. Flere pods kjører samtidig.

## 2. Mentalt bilde

- En jobb er en rad i `jobb` (historikk i `jobb_historikk`). Status: KLAR → FERDIG | FEILET | AVBRUTT (PLUKKET finnes bare i historikken).
- **Eksklusivitetsgruppe = (sak_id, behandling_id, type).** Én jobb om gangen i hver gruppe, sortert etter `neste_kjoring`.
  Jobber uten sak_id og behandling_id er *selvstendige*: de har ingen rekkefølge og kjører parallelt.
- **Ulike typer for samme behandling tilhører ulike grupper og kan kjøre samtidig** (i en annen worker/pod).
  Derfor tar utførere radlåser: `TaSkriveLåsRepository.lås(sakId, behandlingId)` / `låsSak` (`SELECT … FOR UPDATE`,
  alltid sak før behandling) og kaller `verifiserSkrivelås` til slutt (øker `versjon`).
- Scheduler (hvert 100 ms, én pod om gangen via advisory lock) setter `kjorbar = true` på første jobb i hver gruppe som ikke er blokkert
  (maks. 50 per kjøring) og på selvstendige jobber som skal kjøres. En gruppe er blokkert mens den har en FEILET-jobb eller en KLAR+kjorbar-jobb.
- Workers plukker jobber med `KLAR and kjorbar and neste_kjoring <= now()`, sortert etter `prioritet, neste_kjoring` (`FOR UPDATE SKIP LOCKED`).
  `Prioritet`: lavere tall betyr høyere prioritet (KRITISK 10, HØY 50, NORMAL 100, LAV 200, BAKGRUNN 500). Prioritet endrer ikke rekkefølgen innad i en gruppe.

## 3. Transaksjoner, her skjer de vanlige misforståelsene

```
tx1 "jobbPlukk" (holds the jobb row lock)
 ├─ tx2: JobbUtfører.utfør(input)   repositories are bound to tx2's connection; cron jobs plan their next run here
 │       success → commit tx2
 │       exception → rollback tx2 to its LAST SAVEPOINT (if one was set) and COMMIT the rest, rethrow
 └─ markerSomFerdig / markerSomFeilet in tx1 → commit tx1
```
- **At-least-once**: tx2 committer før tx1 markerer jobben som FERDIG. Hvis tx1 feiler etterpå, kjører jobben igjen. Jobber må være idempotente
  (mønster: sjekk om arbeidet allerede er gjort, for eksempel `kjennerTilDokumentFraFør` eller sjekken for eksisterende jobb i `triggProsesserBehandling`).
- **Outbox**: `FlytJobbRepository.leggTil` får bare effekt hvis transaksjonen committer, enten den kommer fra et API-kall, et steg eller en jobb.
  Bruk jobber for sideeffekter mot andre systemer i stedet for å kalle dem direkte.
- **Savepoints**: Bare `BehandlingRepository.markerSavepoint()` setter dem (StegOrkestrator kaller den etter START og
  OPPDATER_FAKTAGRUNNLAG i hvert steg). Derfor beholder en jobb med feil i `flyt.prosesserBehandling` framdriften fra før den feilede
  stegfasen (se flyt-orkestrering §9). Andre jobber rulles helt tilbake.

## 4. Retry og feil

- Forsøk = antall FEILET-rader i `jobb_historikk`; `maksFeilNådd = retries <= antallFeil + 1`. Standard `retries = 3`.
- Uten `retryBackoffTid` plukkes neste forsøk umiddelbart → **3 forsøk på noen sekunder**, deretter status FEILET.
  Med backoff (for eksempel `flyt.iverksettUtbetaling`: 15 retries, 1 min) beholder jobben plassen sin og blokkerer gruppen i mellomtiden.
- En FEILET-jobb blokkerer hele gruppen til den tilbakestilles. `oppgave.retryFeilede` (innebygd i motor, cron 07:00/12:00/15:00/20:00)
  setter alle FEILET-jobber til KLAR. Siden historikken allerede har minst 3 feil, gir hver tilbakestilling **nøyaktig ett** nytt forsøk.
- Spesielt for `flyt.prosesserBehandling`: En KLAR- eller FEILET-jobb for behandlingen gjør at `triggProsesserBehandling` ikke gjør noe,
  at `POST /api/behandling/løs-behov` feiler (`BehandlingUnderProsesseringException`), og at FEILET vises i
  `GET /api/behandling/{ref}/flyt/prosessering`.
- Feiltekst: `jobb_historikk.feilmelding` (stack trace), motor-api `GET /drift/api/jobb/feilende`, sikre logger
  («Feil under prosessering av jobb»). Feil under *plukking* logges som «Feil under plukking av jobber» og forsøkes på nytt hvert 0,5 s.
- En uregistrert `jobb.type` gjør at `JobbType.parse` kaster et unntak under plukking og i `hentJobberForBehandling`. Ikke slett eller
  gi en type nytt navn mens det kan finnes rader. Behold et `object` som ikke gjør noe (se `OpprettJobbForFastsattPeriodeJobbUtfører`).

## 5. Cron-jobber

- `override val cron = CronExpression.create("0 0 7 * * *")` (6 felt, inkludert sekunder) eller `createWithoutSeconds("0 7 * * *")`.
- Ved oppstart planlegger `RetryService.enable()` neste kjøring for hver registrerte cron-type som ikke har en rad med annen status enn FERDIG (og tilbakestiller
  FEILET-rader). Hver vellykkede kjøring planlegger den neste i samme transaksjon. Derfor **slutter en cron-jobb som feiler å kjøre igjen**
  før den tilbakestilles.
- Cron-jobber har ingen sak eller behandling. Et vanlig mønster er at en `batch.OpprettJobbFor…`-cron finner kandidater og legger til én
  `forSak`-jobb per sak (`OpprettJobbForGReguleringJobbUtfører` → `OpprettBehandlingGReguleringJobbUtfører`).

## 6. Skriv en jobb

```kotlin
class MinJobbUtfører(
    private val låsRepository: TaSkriveLåsRepository,
    private val noeService: NoeService,
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val lås = låsRepository.låsSak(sakId)          // only if you mutate sak/behandling state
        noeService.gjørIdempotentArbeid(sakId, input.payload<MinPayload>())
        låsRepository.verifiserSkrivelås(lås)
    }

    companion object : ProvidersJobbSpesifikasjon {
        override val type = "flyt.minJobb"               // persisted in jobb.type: never rename/remove
        override val navn = "Min jobb"
        override val beskrivelse = "Hva og hvorfor"
        // optional: retries, retryBackoffTid (java.time.Duration), prioritet, cron

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) =
            MinJobbUtfører(repositoryProvider.provide(), NoeService(repositoryProvider, gatewayProvider))

        fun nyJobb(sakId: SakId, payload: MinPayload) =
            JobbInput(MinJobbUtfører).forSak(sakId.toLong()).medPayload(payload).medCallId()
    }
}
```
Sjekkliste:
1. Registrer jobben i `ProsesseringsJobber.alle()` (brukes også av `ManuellMotorImpl` i tester).
2. Legg den til med `repositoryProvider.provide<FlytJobbRepository>().leggTil(MinJobbUtfører.nyJobb(...))` i forretningstransaksjonen.
3. Velg gruppering bevisst: `forBehandling(sak, behandling)` serialiserer per behandling+type; `forSak(sak)` per sak+type
   (på tvers av behandlinger, for eksempel rekkefølgen på utbetalinger); uten gruppering kjører jobber parallelt. `medNesteKjøring(t)` utsetter kjøringen, `medPrioritet(...)` setter prioritet for en jobbinstans.
4. Payload: `medPayload(obj)` (JSON) / `input.payload<T>()`; bruk `medParameter(k, v)` / `input.parameter(k)` for små verdier.
5. Sørg for idempotens, og unngå lange eksterne kall mens du holder låser. Bruk en jobb for eksterne kall i steg.
6. Tester (testing-skillen): test utføreren med in-memory-repositories og fakes. Test hele flyten med `motor.kjørJobber()` (§7).

## 7. Tester: `ManuellMotorImpl` (motor-test-utils) skiller seg fra produksjon

- Ingenting kjører før `motor.kjørJobber()` kalles. Da kjører alle jobber som skal kjøres, også jobber som andre jobber har lagt til, til køen er tom.
  Hjelpemetodene i `AbstraktFlytOrkestratorTest` kaller den etter hver handling.
- Bruker V1-spørringen for plukking (ingen scheduler eller `kjorbar`). Jobber med `neste_kjoring` i framtiden kjøres ikke.
- Kjører jobben på **plukketilkoblingen**: en jobb som feiler **rulles ikke tilbake**, og unntaket **fanges og skjules**
  (logges som «Feil under prosessering av jobb»). Hvis en senere assertion feiler på grunn av en uventet tilstand, les testloggen først.
- Cron-jobber planlegges ikke automatisk. Legg eksplisitt til en `JobbInput` for å teste en cron-jobb.

## 8. Drift (se feilsok-behandling-skillen for detaljer)

motor-api (`App.kt` → `motorApi`, prod krever team-/driftsroller): `GET /drift/api/jobb/feilende`, `/planlagte-jobber`,
`/sisteKjørte`, `GET /drift/api/jobb/rekjor/{jobbId}`, `/rekjorAlleFeilede`, `/avbryt/{jobbId}`, `/avbrytAlleFeilede`.
Manuelle SQL-inserts: bruk `kjorbar = false` og la scheduler oppgradere dem. `kjorbar = true` omgår rekkefølgen og
bryter den unike indeksen `UX_JOBB_EKSKLUSIV_AKTIV` hvis gruppen allerede har en aktiv eller FEILET-jobb.
