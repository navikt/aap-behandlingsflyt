---
name: motor-jobber
description: The motor job engine (no.nav.aap.motor from aap-kelvin-komponenter) as used in behandlingsflyt — JobbUtfører/JobbSpesifikasjon, JobbInput (forSak/forBehandling, payload, prioritet), exclusivity groups, scheduler and kjorbar, the two-transaction model with savepoint partial commits, retries/backoff/FEILET blocking, cron jobs, ManuellMotorImpl in tests. Use when creating or changing a job, when a job fails or a behandling is stuck "under prosessering", or when reasoning about ordering, concurrency or idempotency. Keywords — motor, jobb, JobbUtfører, FlytJobbRepository, forbrenningskammer, cron, retry, kjorbar, flyt.prosesserBehandling.
---

# Motor (job engine)

The library is **not in this repo**: `no.nav.aap.kelvin:motor` (+ `motor-api`, `motor-test-utils`), version =
`komponenter` in `gradle/libs.versions.toml`. Source: https://github.com/navikt/aap-kelvin-komponenter (`motor/`).
For exact SQL and internals read [references/internals.md](references/internals.md). To inspect a failing job in an
environment, use the feilsok-behandling skill.

Inventory of this app's jobs (type string, class, cron, retries, grouping):
`python3 .agents/skills/motor-jobber/scripts/jobber.py` · `jobber.py --type flyt.prosesserBehandling`

## 1. Wiring in this app

`App.kt` → `startMotor`: 4 workers ("forbrenningskammer", `AppConfig.ANTALL_WORKERS_FOR_MOTOR`) per pod, own Hikari pool
"motor" (2 connections per worker), `enableV2 = { true }` (scheduler + `kjorbar` column), jobs from
`prosessering/ProsesseringsJobber.alle()`, `RetryService.enable()` at startup, started on `ApplicationStarted`, stopped on
`ApplicationStopping`. Several pods run concurrently.

## 2. Mental model

- A job is a row in `jobb` (history in `jobb_historikk`). Status: KLAR → FERDIG | FEILET | AVBRUTT (PLUKKET appears only in history).
- **Exclusivity group = (sak_id, behandling_id, type).** Inside a group: one job at a time, ordered by `neste_kjoring`.
  Jobs with neither sak_id nor behandling_id are *selvstendige*: no ordering, run in parallel.
- **Different types for the same behandling are different groups and may run at the same time** (other worker/pod).
  That is why utførere take row locks: `TaSkriveLåsRepository.lås(sakId, behandlingId)` / `låsSak` (`SELECT … FOR UPDATE`,
  always sak before behandling) and `verifiserSkrivelås` at the end (bumps `versjon`).
- Scheduler (every 100 ms, one pod at a time via advisory lock) sets `kjorbar = true` on the head of each unblocked group
  (≤ 50 per run) and on due selvstendige jobs. A group is blocked while it has a FEILET job or a KLAR+kjorbar job.
- Workers pick `KLAR and kjorbar and neste_kjoring <= now()` ordered by `prioritet, neste_kjoring` (`FOR UPDATE SKIP LOCKED`).
  `Prioritet`: lower = sooner (KRITISK 10, HØY 50, NORMAL 100, LAV 200, BAKGRUNN 500); it never reorders within a group.

## 3. Transactions — the part people get wrong

```
tx1 "jobbPlukk" (holds the jobb row lock)
 ├─ tx2: JobbUtfører.utfør(input)   repositories are bound to tx2's connection; cron jobs plan their next run here
 │       success → commit tx2
 │       exception → rollback tx2 to its LAST SAVEPOINT (if one was set) and COMMIT the rest, rethrow
 └─ markerSomFerdig / markerSomFeilet in tx1 → commit tx1
```
- **At-least-once**: tx2 commits before tx1 marks FERDIG; if tx1 fails afterwards the job runs again. Jobs must be idempotent
  (pattern: check "already done", e.g. `kjennerTilDokumentFraFør`, or `triggProsesserBehandling`'s existing-job check).
- **Outbox**: `FlytJobbRepository.leggTil` in any transaction (API call, step, job) only takes effect if that transaction
  commits. Use jobs for side effects towards other systems instead of calling them inline.
- **Savepoints**: only `BehandlingRepository.markerSavepoint()` sets them (called by StegOrkestrator after START and
  OPPDATER_FAKTAGRUNNLAG of each step). So a failing `flyt.prosesserBehandling` keeps the progress made before the failing
  step phase (see flyt-orkestrering §9). Other jobs roll back completely.

## 4. Retries and failures

- Attempts = number of FEILET rows in `jobb_historikk`; `maksFeilNådd = retries <= antallFeil + 1`. Default `retries = 3`.
- Without `retryBackoffTid` the next attempt is picked immediately → **3 attempts within seconds**, then status FEILET.
  With backoff (e.g. `flyt.iverksettUtbetaling`: 15 retries, 1 min) the job keeps its slot and blocks its group meanwhile.
- A FEILET job blocks its whole group until reset. `oppgave.retryFeilede` (motor built-in, cron 07:00/12:00/15:00/20:00)
  sets ALL FEILET jobs to KLAR; because history already holds ≥ 3 failures, each reset gives **exactly one** more attempt.
- `flyt.prosesserBehandling` specifically: a KLAR or FEILET job for the behandling makes `triggProsesserBehandling` a no-op,
  makes `POST /api/behandling/løs-behov` fail (`BehandlingUnderProsesseringException`) and shows FEILET in
  `GET /api/behandling/{ref}/flyt/prosessering`.
- Error text: `jobb_historikk.feilmelding` (stack trace), motor-api `GET /drift/api/jobb/feilende`, secure logs
  ("Feil under prosessering av jobb"). Errors while *picking* are logged as "Feil under plukking av jobber" and retried every 0.5 s.
- An unregistered `jobb.type` makes `JobbType.parse` throw while picking and in `hentJobberForBehandling`. Never delete or
  rename a type while rows may exist — keep a no-op `object` (see `OpprettJobbForFastsattPeriodeJobbUtfører`).

## 5. Cron jobs

- `override val cron = CronExpression.create("0 0 7 * * *")` (6 fields incl. seconds) or `createWithoutSeconds("0 7 * * *")`.
- At startup `RetryService.enable()` plans the next run for every registered cron type without a non-FERDIG row (and resets
  FEILET ones). Each successful run plans the next one in the same transaction, so **a failing cron job stops recurring**
  until it is reset.
- Cron jobs have no sak/behandling. Typical pattern: a `batch.OpprettJobbFor…` cron finds candidates and enqueues one
  `forSak` job per sak (`OpprettJobbForGReguleringJobbUtfører` → `OpprettBehandlingGReguleringJobbUtfører`).

## 6. Writing a job

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
Checklist:
1. Register in `ProsesseringsJobber.alle()` (also used by tests' `ManuellMotorImpl`).
2. Enqueue with `repositoryProvider.provide<FlytJobbRepository>().leggTil(MinJobbUtfører.nyJobb(...))` inside the business transaction.
3. Pick grouping on purpose: `forBehandling(sak, behandling)` serialises per behandling+type; `forSak(sak)` per sak+type
   (across behandlinger — e.g. utbetaling order); none = parallel. `medNesteKjøring(t)` delays, `medPrioritet(...)` per instance.
4. Payload: `medPayload(obj)` (JSON) / `input.payload<T>()`; small values `medParameter(k, v)` / `input.parameter(k)`.
5. Idempotent, and no long external calls while holding locks if avoidable. For external calls inside steps use a job.
6. Tests (testing skill): unit-test the utfører with in-memory repos/fakes; end-to-end via `motor.kjørJobber()` (§7).

## 7. Tests: `ManuellMotorImpl` (motor-test-utils) differs from production

- Nothing runs until `motor.kjørJobber()`; it then runs every due job, including jobs enqueued by jobs, until none are left.
  `AbstraktFlytOrkestratorTest` helpers call it after each action.
- Uses the V1 pick query (no scheduler / `kjorbar`); jobs with `neste_kjoring` in the future are not run.
- Runs the job on the **pick connection**: a failing job is **not rolled back** and its exception is **swallowed**
  (logged "Feil under prosessering av jobb"). Symptom: a later assertion fails with puzzling state — read the test log first.
- Cron jobs are not planned automatically; enqueue a `JobbInput` explicitly to test one.

## 8. Operations (details in feilsok-behandling skill)

motor-api (`App.kt` → `motorApi`, prod requires team/drift roles): `GET /drift/api/jobb/feilende`, `/planlagte-jobber`,
`/sisteKjørte`, `GET /drift/api/jobb/rekjor/{jobbId}`, `/rekjorAlleFeilede`, `/avbryt/{jobbId}`, `/avbrytAlleFeilede`.
Manual SQL inserts: use `kjorbar = false` and let the scheduler promote them — `kjorbar = true` bypasses ordering and
violates the unique index `UX_JOBB_EKSKLUSIV_AKTIV` if the group already has an active/FEILET job.
