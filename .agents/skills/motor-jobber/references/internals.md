# Motor internals (verified against aap-kelvin-komponenter, motor unchanged 2.0.191 → 2.0.197)

Files in the komponenter repo: `motor/src/main/kotlin/no/nav/aap/motor/`
`Motor.kt` (MotorImpl: Forbrenningskammer, Scheduler, Watchdog, MetricsUpdater), `JobbRepository.kt` (all SQL),
`JobbInput.kt`, `JobbSpesifikasjon.kt`, `Prioritet.kt`, `JobbType.kt`, `FlytJobbRepository.kt`, `retry/*`, `jobbarkiv/*`,
`../../../../../../motor-api/.../MotorApi.kt`, `motor-test-utils/.../ManuellMotorImpl.kt`.
dbconnect: `dbconnect/src/main/kotlin/no/nav/aap/komponenter/dbconnect/{DBConnect.kt,DBTransaction.kt,DBConnection.kt}`.

## Worker loop (Forbrenningskammer.run)

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

## dbconnect transaction semantics (why failures commit partial work)

```kotlin
// DBTransaction.transaction
try { autoCommitOff(); val r = block(dbConnection); dbConnection.commit(); return r }
catch (e: Throwable) { dbConnection.rollback(); throw e }   // rollback(): to `savepoint` if markerSavepoint() was called
finally { dbConnection.autoCommitOn() }                     // JDBC: switching autocommit on COMMITS the open transaction
```
`DBConnection.markerSavepoint()` keeps only the latest savepoint (one field). In behandlingsflyt only
`BehandlingRepositoryImpl.markerSavepoint()` calls it.

## Pick query V2 (prod)

```sql
select … from jobb
where status = 'KLAR' and kjorbar and neste_kjoring <= now()
order by prioritet, neste_kjoring
for update skip locked limit 1
```

## Scheduler (V2, every 100 ms, `pg_try_advisory_xact_lock(0x4D4F544F52534B4A)` so one pod at a time)

1. Selvstendige: `update jobb set kjorbar = true where status='KLAR' and sak_id is null and behandling_id is null and not kjorbar and neste_kjoring <= now()`.
2. Ekskluderende: per (sak_id, behandling_id, type) group that has no row with `status='FEILET' or (status='KLAR' and kjorbar)`,
   promote the KLAR row with the lowest `neste_kjoring` (≤ now), max 50 groups per run.
`kjorbar` only goes false → true. A backoff retry moves `neste_kjoring` forward but keeps `kjorbar = true` (keeps the slot).
Unique index `UX_JOBB_EKSKLUSIV_AKTIV (coalesce(sak_id,-1), coalesce(behandling_id,-1), type) where status='FEILET' or (status='KLAR' and kjorbar)` (flyway V1.335).

## V1 pick (used by ManuellMotorImpl unless enableV2)

`distinct on (sak_id, behandling_id, type)` over KLAR+FEILET rows ordered by neste_kjoring; picks the head only if it is KLAR
(a FEILET head blocks the group) + selvstendige; `neste_kjoring < now()`; ordered by `neste_kjoring` (no priority).

## Retry arithmetic

`antallFeil` = count of FEILET rows in `jobb_historikk` for the job (all time). `maksFeilNådd(): retries <= antallFeil + 1`.
`markerSomFeilet`: if maks → `status = FEILET`; else if `retryBackoffTid != null` → `neste_kjoring = now + backoff`;
always inserts a FEILET history row with the stack trace in `feilmelding`.
`RekjørFeiledeJobb` (`oppgave.retryFeilede`): `UPDATE jobb SET status='KLAR' WHERE status='FEILET'` (+ KLAR history rows).

## JobbInput API (selection)

`JobbInput(spec)` · `.forBehandling(sakId: Long, behandlingId: Long)` · `.forSak(sakId: Long)` · `.medPayload(T)` /
`.medPayload(String?)` · `.medParameter(k, v)` · `.medNesteKjøring(LocalDateTime)` · `.medPrioritet(Int)` · `.medCallId()`
Reading: `input.sakId()`, `behandlingId()` (throw if null; `…OrNull()` variants), `payload<T>()`, `parameter(k)`,
`optionalParameter(k)`, `antallRetriesForsøkt()`, `jobbId()`.
`JobbSpesifikasjon` flavours: `ProvidersJobbSpesifikasjon` (RepositoryProvider + GatewayProvider — used here),
`ProviderJobbSpesifikasjon`, `ConnectionJobbSpesifikasjon`, legacy `Jobb`.
`FlytJobbRepository`: `leggTil`, `hentJobberForBehandling(id)` / `hentJobberForSak(id)` (KLAR + FEILET only),
`hentFeilmeldingForOppgave(id)`, `hentJobberMedHistorikkForSak(id)`.

## Archive

`jobber.arkiverFerdigstilte` (cron 01:00) moves FERDIG jobs to `jobb_arkiv` / `jobb_historikk_arkiv`. When investigating old
runs, query the archive tables too.

## Metrics (Prometheus)

`motor_antall_jobber_klar`, `motor_antall_jobber_feilet`, `motor_siste_plukk_timestamp_seconds{forbrenningskammer,jobb_type}`,
`motor_scheduler_siste_kjoring_timestamp_seconds`, job timers per type, failure counters when max retries is reached.
Watchdog restarts dead workers every minute and logs ERROR if the scheduler hasn't completed for 2 minutes.

## motor-api endpoints (`/drift/api/jobb`)

GET `/feilende`, `/feilende/antall`, `/planlagte-jobber`, `/sisteKjørte`, `/rekjor/{jobbId}`, `/rekjorAlleFeilede`,
`/avbryt/{jobbId}` (also POST with body), `/avbrytAlleFeilede`; POST `/{jobbId}/kjor`, `/{jobbId}/leggTilKommentar`.
