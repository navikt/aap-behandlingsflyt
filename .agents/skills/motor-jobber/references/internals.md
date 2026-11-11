# Internt i motor (verifisert mot aap-kelvin-komponenter, motor uendret fra 2.0.191 → 2.0.197)

Filer i komponenter-repoet: `motor/src/main/kotlin/no/nav/aap/motor/`
`Motor.kt` (MotorImpl: Forbrenningskammer, Scheduler, Watchdog, MetricsUpdater), `JobbRepository.kt` (all SQL),
`JobbInput.kt`, `JobbSpesifikasjon.kt`, `Prioritet.kt`, `JobbType.kt`, `FlytJobbRepository.kt`, `retry/*`, `jobbarkiv/*`,
`../../../../../../motor-api/.../MotorApi.kt`, `motor-test-utils/.../ManuellMotorImpl.kt`.
dbconnect: `dbconnect/src/main/kotlin/no/nav/aap/komponenter/dbconnect/{DBConnect.kt,DBTransaction.kt,DBConnection.kt}`.

## Worker-løkke (Forbrenningskammer.run)

```
while not stopped:
  repeat:
    dataSource.transaction("jobbPlukkTransaction") { conn ->           // tx1
      job = JobbRepository(conn).plukkJobbV2()                           // inserts PLUKKET history row
      if job == null: stop repeating
      else utfør(job, conn)
    }
  (exceptions here → log "Feil under plukking av jobber")
  sleep 500 ms
utfør(job, conn):
  try {
    dataSource.transaction { nyConn ->                                   // tx2
      MDC(jobbid, jobbType, sakId, behandlingId, callId + BehandlingsflytLogInfoProvider fields)
      job.kjør(nyConn, repositoryRegistry, gatewayProvider)              // konstruer(provider(nyConn), gw).utfør(input)
      if cron: JobbRepository(nyConn).leggTil(next run)
    }
    JobbRepository(conn).markerSomFerdig(job)
  } catch (t) {
    JobbRepository(conn).markerSomFeilet(job, t)   // FEILET status only if maksFeilNådd; else maybe neste_kjoring += backoff
  }
```

## Transaksjonssemantikk i dbconnect (hvorfor feil committer deler av arbeidet)

```kotlin
// DBTransaction.transaction
try { autoCommitOff(); val r = block(dbConnection); dbConnection.commit(); return r }
catch (e: Throwable) { dbConnection.rollback(); throw e }   // rollback(): to `savepoint` if markerSavepoint() was called
finally { dbConnection.autoCommitOn() }                     // JDBC: switching autocommit on COMMITS the open transaction
```
`DBConnection.markerSavepoint()` beholder bare det siste savepointet (ett felt). I behandlingsflyt er det bare
`BehandlingRepositoryImpl.markerSavepoint()` kaller den.

## V2-spørring for plukking (prod)

```sql
select … from jobb
where status = 'KLAR' and kjorbar and neste_kjoring <= now()
order by prioritet, neste_kjoring
for update skip locked limit 1
```

## Scheduler (V2, hvert 100 ms, `pg_try_advisory_xact_lock(0x4D4F544F52534B4A)` slik at én pod kjører om gangen)

1. Selvstendige: `update jobb set kjorbar = true where status='KLAR' and sak_id is null and behandling_id is null and not kjorbar and neste_kjoring <= now()`.
2. Ekskluderende: For hver gruppe (sak_id, behandling_id, type) uten en rad med `status='FEILET' or (status='KLAR' and kjorbar)`,
   sett KLAR-raden med lavest `neste_kjoring` (≤ now) som kjorbar, maks. 50 grupper per kjøring.
`kjorbar` endres bare fra false → true. Et retry med backoff flytter `neste_kjoring` framover, men beholder `kjorbar = true` (plassen beholdes).
Unik indeks `UX_JOBB_EKSKLUSIV_AKTIV (coalesce(sak_id,-1), coalesce(behandling_id,-1), type) where status='FEILET' or (status='KLAR' and kjorbar)` (flyway V1.335).

## V1-plukking (brukes av ManuellMotorImpl hvis ikke enableV2 er aktivert)

`distinct on (sak_id, behandling_id, type)` over KLAR- og FEILET-rader sortert etter neste_kjoring; plukker den første raden bare hvis den er KLAR
(en FEILET-rad først blokkerer gruppen) + selvstendige; `neste_kjoring < now()`; sortert etter `neste_kjoring` (ingen prioritet).

## Retry-beregning

`antallFeil` = antall FEILET-rader i `jobb_historikk` for jobben (alle forsøk). `maksFeilNådd(): retries <= antallFeil + 1`.
`markerSomFeilet`: Hvis maks er nådd → `status = FEILET`; ellers, hvis `retryBackoffTid != null` → `neste_kjoring = now + backoff`;
setter alltid inn en FEILET-historikkrad med stack trace i `feilmelding`.
`RekjørFeiledeJobb` (`oppgave.retryFeilede`): `UPDATE jobb SET status='KLAR' WHERE status='FEILET'` (+ KLAR-historikkrader).

## JobbInput API (utvalg)

`JobbInput(spec)` · `.forBehandling(sakId: Long, behandlingId: Long)` · `.forSak(sakId: Long)` · `.medPayload(T)` /
`.medPayload(String?)` · `.medParameter(k, v)` · `.medNesteKjøring(LocalDateTime)` · `.medPrioritet(Int)` · `.medCallId()`
Lesing: `input.sakId()`, `behandlingId()` (kaster unntak hvis null; `…OrNull()`-varianter), `payload<T>()`, `parameter(k)`,
`optionalParameter(k)`, `antallRetriesForsøkt()`, `jobbId()`.
Varianter av `JobbSpesifikasjon`: `ProvidersJobbSpesifikasjon` (RepositoryProvider + GatewayProvider, brukes her),
`ProviderJobbSpesifikasjon`, `ConnectionJobbSpesifikasjon`, eldre `Jobb`.
`FlytJobbRepository`: `leggTil`, `hentJobberForBehandling(id)` / `hentJobberForSak(id)` (bare KLAR + FEILET),
`hentFeilmeldingForOppgave(id)`, `hentJobberMedHistorikkForSak(id)`.

## Arkiv

`jobber.arkiverFerdigstilte` (cron 01:00) flytter FERDIG-jobber til `jobb_arkiv` / `jobb_historikk_arkiv`. Når du undersøker eldre
kjøringer, må du også spørre arkivtabellene.

## Metrikker (Prometheus)

`motor_antall_jobber_klar`, `motor_antall_jobber_feilet`, `motor_siste_plukk_timestamp_seconds{forbrenningskammer,jobb_type}`,
`motor_scheduler_siste_kjoring_timestamp_seconds`, jobbtimere per type og feiltellere når maksimalt antall retry er nådd.
Watchdog starter workers som har stoppet, på nytt hvert minutt, og logger ERROR hvis scheduler ikke har fullført på 2 minutter.

## Endepunkter i motor-api (`/drift/api/jobb`)

GET `/feilende`, `/feilende/antall`, `/planlagte-jobber`, `/sisteKjørte`, `/rekjor/{jobbId}`, `/rekjorAlleFeilede`,
`/avbryt/{jobbId}` (også POST med body), `/avbrytAlleFeilede`; POST `/{jobbId}/kjor`, `/{jobbId}/leggTilKommentar`.
