# AGENTS.md — aap-behandlingsflyt

Backend of Kelvin, NAV's case-processing system for arbeidsavklaringspenger (AAP). It defines the *behandlingsflyt*
(ordered steps a case goes through), drives each behandling through those steps with a database-backed state machine
executed by background jobs, and serves REST APIs to the saksbehandling frontend. Kotlin 2.4 / JVM 25, Ktor, PostgreSQL +
Flyway, and NAV's `no.nav.aap.kelvin:*` "komponenter" libraries (`motor`, `dbconnect`, `tidslinje`, `httpklient`, …).
Code and domain language are Norwegian (identifiers keep æ/ø/å); these docs are English and use Norwegian terms verbatim.

## Modules

| Module | Contents |
|---|---|
| `kontrakt` | Library published to other Kelvin apps: `StegType`, `StegGruppe`, `Definisjon`/`AvklaringsbehovKode`, behandling/sak `Status`, `TypeBehandling`, event DTOs. Changes affect aap-tilgang, aap-oppgave, aap-statistikk and the frontend (they must bump the version). |
| `behandlingsflyt` | Domain + engine. Repository and gateway **interfaces** live here. Package root `no.nav.aap.behandlingsflyt`; newer vertical feature packages `no.nav.aap.{bistandsbehov,meldeplikt,overgangarbeid}`. |
| `repository` | Postgres repository impls, Flyway migrations (`src/main/resources/flyway`), HTTP gateways (`integrasjon/`), `postgresRepositoryRegistry`, `defaultGatewayProvider()` |
| `api` | Ktor routes (`*Api.kt`), response DTOs, Kafka consumers (`hendelse/kafka`) |
| `app` | `App.kt` wiring (data sources, Motor, routes, Kafka), backfills; tests incl. `TestApp` |
| `lib-test` | Test fixtures: `FakeServers` (HTTP fakes), `FakePersoner`, fake Unleash, in-memory repositories, `testGatewayProvider()` |
| `docs`, `build-logic` | Dokka site + `docs/playbook.md` (incident procedures); Gradle convention plugin `aap.conventions` |

Key paths under `behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/`: `flyt/` (engine), `forretningsflyt/behandlingstyper/`
(flow definitions; `Revurdering.kt` is THE flow, `Førstegangsbehandling` reuses it), `forretningsflyt/steg/` (steps),
`behandling/avklaringsbehov/` (+ `løsning/`, `løser/`), `behandling/vilkår/`, `faktagrunnlag/` (informasjonskrav, grunnlag),
`prosessering/` (jobs, `ProsesseringsJobber.alle()`), `hendelse/mottak|avløp/` (events in/out), `sakogbehandling/`
(Sak, Behandling, BehandlingService, Vurderingsbehov, locks), `periodisering/` (VurderingType per step).

## Build and test (details: `testing`)

- Always use the wrapper `./gradlew` from the repo root, never a globally installed `gradle`.
- **Never run the whole test suite** (`./gradlew test`/`check`/`build`, or an unfiltered `:behandlingsflyt:test`,
  `:repository:test`, `:api:test`, `:app:test`): it is slow and times out in agent sandboxes. Run one class or a small
  batch per invocation with `--tests`, one invocation per tool call; flyt-tests and `app` integration tests at most ~3
  classes per run.
  `python3 .agents/skills/testing/scripts/kjor_test.py <Klasse>` prints the command; `--batch flyt` splits many.
- Example: `./gradlew :behandlingsflyt:test --tests 'no.nav.aap.behandlingsflyt.flyt.AlderFlytTest'`. Compile without
  running tests: `./gradlew compileTestKotlin`.
- Needs a JDK (toolchain JDK 25) and Docker for Testcontainers (Colima: env vars in README). Check: `kjor_test.py --sjekk-miljo`.
- CI runs `./gradlew :<module>:check` per module (api, app, behandlingsflyt, kontrakt, lib-test, repository).
- `./gradlew detekt` (report only). Local app with fakes: `./gradlew runTestApp` → http://localhost:8080/swagger-ui/index.html.
- If you could not compile or run tests, say so explicitly; never claim they pass.

## How a behandling moves (details: `flyt-orkestrering`)

1. Document/event in → job `hendelse.håndterer` → `BehandlingService.finnEllerOpprettBehandling` creates a behandling or adds
   *vurderingsbehov* to the open one → enqueues job `flyt.prosesserBehandling`.
2. That job runs `FlytOrkestrator.forberedOgProsesserBehandling`: *forbered* (resolve ventebehov, refresh informasjonskrav,
   maybe rewind) then *prosesser* (steps in flow order; each step: START → OPPDATER_FAKTAGRUNNLAG → UTFØRER → AVKLARINGSPUNKT → AVSLUTTER).
3. It stops at the first step that has an open avklaringsbehov to be solved in that step, or closes the behandling at the end.
4. Saksbehandler solves via `POST /api/behandling/løs-behov` → løser stores the vurdering, behov AVSLUTTET, a new
   `flyt.prosesserBehandling` job is enqueued (HTTP 202). The flow continues asynchronously.

## Rules that are easy to get wrong (all verified in code)

1. A step's `utfør` runs again every time the flow passes or resumes there (from START): make it idempotent and reconcile
   avklaringsbehov through `AvklaringsbehovService`, never by hand.
2. Steps don't stop the flow; avklaringsbehov do (checked in AVKLARINGSPUNKT). Every step in a flow always executes;
   `vurderingsbehovRelevanteForSteg` only filters the kontekst and drives rewinds.
3. Tilbakeføring (rewind) only writes TILBAKEFØRT step history — no step code runs, no data is undone.
4. `Status.erAvsluttet()` is true for IVERKSETTES too (behandling) and for AVBRUTT (avklaringsbehov).
5. `Førstegangsbehandling.flyt()` == `Revurdering.flyt()`. For business meaning use `BehandlingService.utledFaktiskBehandlingstype()`.
6. `behandling.flyt()` is a new `BehandlingFlyt` positioned on the FIRST step; call `forberedFlyt(behandling.aktivtSteg())` first.
7. A failing `flyt.prosesserBehandling` job commits work up to the last savepoint (after START/OPPDATER_FAKTAGRUNNLAG of each
   step); it does not roll back everything. Jobs run at-least-once: make them idempotent.
8. Motor serialises jobs only per (sak_id, behandling_id, type). A KLAR/FEILET `flyt.prosesserBehandling` blocks new triggers and
   saksbehandling (`BehandlingUnderProsesseringException`); default 3 immediate attempts, then FEILET until a retry cron.
9. `Informasjonskrav.hentData` runs in parallel threads: no DB there; `klargjør`/`oppdater`: no HTTP.
10. Persisted enum names (StegType, Definisjon, Vurderingsbehov, InformasjonskravNavn, ÅrsakTilOpprettelse, job `type`
    strings…) must never be renamed or removed — deprecate. Changing step order/removing steps can break open behandlinger.
11. Revurdering copies grunnlag shallowly (`Repository.kopier`: new grunnlag row → shared child rows): never UPDATE child rows.
    Avklaringsbehov, vedtak, brev and logs are NOT copied (`behandlingsflyt/behandlingsflyt.md` says otherwise; it is wrong).
12. New `BehandlingsflytFeature` → also add it to `LokalUnleash` (lib-test), which throws on unknown toggles.
13. In tests `ManuellMotorImpl.kjørJobber()` swallows job exceptions ("Feil under prosessering av jobb") — check the log when a
    flow test fails strangely.
14. JVM and DB session time zone must be Europe/Oslo; `Periode` is inclusive at both ends; `Tid.MAKS` = 2999-01-01.

## Conventions

- Wiring without DI framework: primary constructor takes dependencies (testable), secondary
  `constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider)` builds them via
  `repositoryProvider.provide()` / `gatewayProvider.provide()`.
- Steps: `class X : BehandlingSteg` + `companion object : FlytSteg { konstruer(); type() }`; flows reference the companion.
  Informasjonskrav: `companion object : Informasjonskravkonstruktør`. Jobs: `companion object : ProvidersJobbSpesifikasjon`,
  registered in `ProsesseringsJobber.alle()`. Repositories: `companion object : Factory<…>`, registered in
  `PostgresRepositoryRegistry.kt`. Gateways: registered in `defaultGatewayProvider()` and `testGatewayProvider()`.
- Steps branch on `kontekst.vurderingType` with an exhaustive `when` (no `else`); use `TidligereVurderinger` to skip work
  when an earlier step already gives avslag.
- Periodised data uses `Tidslinje`/`Periode` from komponenter (`map2`/`outerJoin` are outer joins).
- Keep `public` modifiers in `kontrakt` (explicit API mode). Flyway migrations are immutable once merged.
- Match the surrounding style; comments only where behaviour is non-obvious.
- Name boolean helpers after exactly what they check (e.g. `harIkkeMigrertKrav`, not `harIkkeVurderteMigrerteKrav`).
- Every commit message starts with a tag in brackets linking the work to a goal, e.g. `[MIGRERING] …`. The team uses a
  fixed set of specific tags: NEVER invent or guess a tag (not even for a separate or unrelated commit). Always ask the
  user which tag to use for each commit, and show the full commit message for approval before committing. Commit
  messages are always written in Norwegian (bokmål).

## Where to find things

| Need | Look at |
|---|---|
| Steps of a flow, step ↔ class, who reacts to a vurderingsbehov | `flyt.py --flyt Revurdering`, `--steg X`, `--vurderingsbehov Y`; generated `.agents/skills/flyt-orkestrering/references/flyter.md` |
| Avklaringsbehov codes, steps, roles, løsning/løser | `avklaringsbehov.py --kode 5003`; generated `.agents/skills/avklaringsbehov/references/definisjoner.md` |
| Job types | `jobber.py`; `prosessering/ProsesseringsJobber.kt` |
| Feature toggles | `behandlingsflyt/.../unleash/FeatureToggle.kt` (`BehandlingsflytFeature`) |
| API routes | `app/.../App.kt` (`routing { apiRouting { … } }`), implementations in `api/` |
| Env config per environment | `.nais/dev.yaml`, `.nais/prod.yaml` |
| motor/dbconnect/tidslinje source | not in this repo: github.com/navikt/aap-kelvin-komponenter, version `komponenter` in `gradle/libs.versions.toml` |
| Incident procedures | `docs/playbook.md` |

## Core glossary (more in `aap-domene`)

sak = a person's AAP case · behandling = one processing of a sak · steg = step · flyt = ordered steps · avklaringsbehov =
human decision/wait point (stops the flow) · løsning/løser = submitted decision / code storing it · vurdering = assessment ·
vurderingsbehov = reason to (re)assess, drives VurderingType and rewinds · informasjonskrav = register/document fetcher ·
grunnlag = stored facts per behandling · vilkår = legal condition · tilbakeføring = rewind · beslutter/totrinn = second-person
approval · kvalitetssikring = quality check · NAY = national unit (SAKSBEHANDLER_NASJONAL) · Nav-kontor = local office ·
iverksette = execute the vedtak · motor = job engine library · forbrenningskammer = motor worker thread.

## Keeping these docs true

After changing flows, steps, Definisjon or jobs, refresh the generated references:
`python3 .agents/skills/flyt-orkestrering/scripts/flyt.py --check .agents/skills/flyt-orkestrering/references/flyter.md`
and `python3 .agents/skills/avklaringsbehov/scripts/avklaringsbehov.py --check .agents/skills/avklaringsbehov/references/definisjoner.md`
(regenerate with `--markdown > <file>` when stale). Update a skill when you find it wrong — code is the source of truth.

Always update these agent instructions (this file or the relevant skill) when an agent forgot something or the user had
to ask for a fix, so the same mistake is not repeated.
