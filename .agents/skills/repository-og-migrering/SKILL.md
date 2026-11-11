---
name: repository-og-migrering
description: Persistence in aap-behandlingsflyt — repository interfaces vs impls and registries, the grunnlag/aktiv versioning pattern with shared immutable child rows, kopier (revurdering) and slett (GDPR) semantics and which repositories do NOT copy, Flyway migrations (naming, next number), dbconnect usage, row locks and behandling versjon, time zone rules. Use when adding/changing tables, repositories or SQL, when data "disappears" or "leaks" between behandlinger, or when writing repository tests. Keywords — repository, RepositoryImpl, grunnlag, aktiv, kopier, slett, flyway, migrering, dbconnect, DBConnection, transaction, lås, versjon.
---

# Repositories, data model and migrations

Tools: `python3 .agents/skills/repository-og-migrering/scripts/repo.py`
- `neste-migrering [--navn beskrivelse]` → next Flyway file name
- `oversikt [--filter X]` → every repository interface → impl, registered (PG/MEM), `kopier`/`slett` impl | noop | n/a
- `sjekk XRepository` → wiring check for one repository

## 1. Layers and wiring

| What | Where |
|---|---|
| Interface (domain) | `behandlingsflyt/src/main/kotlin/…` (or feature package), extends `no.nav.aap.lookup.repository.Repository` (adds `kopier` + `slett`) or plain `no.nav.aap.komponenter.repository.Repository` (no copy/delete: sak-level data, logs, jobs) |
| Impl (SQL) | `repository/src/main/kotlin/…` (same package as the domain area or `…repository.<area>`), `class XRepositoryImpl(private val connection: DBConnection) : XRepository` + `companion object : Factory<XRepositoryImpl>` (`no.nav.aap.lookup.repository.Factory`). A few legacy impls live in `behandlingsflyt` (`MottattDokumentRepositoryImpl`, `GjenopptakRepositoryImpl`) |
| Production registry | `repository/.../PostgresRepositoryRegistry.kt` → `.register<XRepositoryImpl>()` (registered per marker interface it implements) |
| Test registry | `lib-test/.../inmemoryrepo/InMemoryRepositoryRegistry.kt` (+ `InMemoryXRepository`) for unit tests without DB |
| Usage | `repositoryProvider.provide<XRepository>()` — a `RepositoryProvider` is bound to ONE connection/transaction and caches instances; never keep repositories across transactions |

`GrunnlagKopiererImpl` and the GDPR delete in `SøknadSteg`/`TrekkKlageSteg` iterate `repositoryProvider.provideAlle()` and call
`kopier`/`slett` on every `no.nav.aap.lookup.repository.Repository` — your impl is included automatically once registered
(an impl registered under two marker interfaces is called twice: keep `kopier` idempotent or a no-op).

## 2. The grunnlag pattern (versioned per behandling, structurally shared)

```
X_GRUNNLAG(id, behandling_id, <child>_id, aktiv, opprettet_tid)     ← one ACTIVE row per behandling
X_VURDERINGER(id) / X_VURDERING(id, <set>_id, …)                    ← child rows, immutable once written
```
- `lagre(behandlingId, …)`: set the active grunnlag row `aktiv = false`, insert new child rows, insert a new grunnlag row.
  Old rows stay (history).
- `hentHvisEksisterer(behandlingId)`: `WHERE behandling_id = ? AND aktiv`.
- `kopier(fra, til)` (called when a revurdering is created): insert a new grunnlag row for `til` **pointing to the same child
  rows**. Child rows are therefore shared by several behandlinger → **never UPDATE or DELETE child rows in place**; write new
  ones and repoint.
- `slett(behandlingId)`: hard-delete personal data for that behandling (idempotent). Only called for a førstegangsbehandling
  whose søknad is trukket, or a trukket klage — implementations may assume no later behandling shares the rows.
- Periodised vurderinger: the grunnlag of a behandling contains all gjeldende vurderinger including those inherited from
  earlier behandlinger; each carries `vurdertIBehandling`. Løsere typically save `forrige behandling's vurderinger + nye`
  (see `AvklarSykdomLøser`). "New in this behandling" = `vurdertIBehandling == behandlingId`.

## 3. What is (not) carried into a revurdering

`opprettRevurdering` runs `kopier(forrige → ny)` for every repository. **Not copied** (`kopier` no-op or n/a): avklaringsbehov,
vedtak, brevbestilling, behandling/steg_historikk, kontekst-logg (`ContextRepository`), informasjonskrav_oppdatert, PIP,
klage-specific grunnlag, avbrutt revurdering, uføre-søknad, sak/person, mellomlagring, Arena-migrering, SamId, … — run
`repo.py oversikt` for the current list. (The Dokka page `behandlingsflyt/behandlingsflyt.md` claims vedtak, avklaringsbehov
and brev are copied — the code says otherwise.) Klage, SvarFraAndreinstans and OppfølgingsBehandling start with
`forrigeBehandlingId = null` and copy nothing; Aktivitetsplikt copies from the previous behandling of the same type.

## 4. Flyway

- Files: `repository/src/main/resources/flyway/V1.<n>__<beskrivelse>.sql` (one oddball `V1.14.1__…` exists). Next number:
  `repo.py neste-migrering`. Ordering is numeric (V1.9 < V1.10).
- Applied at app startup (`Migrering.migrate(fellesDataSource)` in `App.kt`) and by `TestDataSource()` in tests (classpath
  location `flyway`). Never edit a merged migration — add a new one. Write migrations that are safe while the previous app
  version is still running (rolling deploy, several pods): add nullable columns / defaults first, remove later.
- Conventions: snake_case names; enums stored as their `name` (`setEnumName`/`getEnum`) — so enum constants must never be
  renamed; `behandling.type` stores `TypeBehandling.identifikator()` ("ae0034"), not the enum name; periods as `daterange`
  (`setPeriode`/`getPeriode`); timestamps `TIMESTAMP(3)` without time zone, written as Oslo local time.
- Key tables: `sak`, `behandling` (`status`, `type`, `versjon`, `forrige_id`, `aarsak_til_opprettelse`), `steg_historikk`
  (one `aktiv` row per behandling), `avklaringsbehov` (unique per behandling+definisjon) + `avklaringsbehov_endring`,
  `vurderingsbehov` + `behandling_aarsak`, `mottatt_dokument`, `informasjonskrav_oppdatert`, `vilkar_resultat`/`vilkar`/`vilkar_periode`,
  `jobb` + `jobb_historikk` (+ `_arkiv`).

## 5. dbconnect essentials (`no.nav.aap.komponenter.dbconnect`)

```kotlin
dataSource.transaction { connection -> … }                    // commit on success; readOnly = true for reads
connection.execute(sql) { setParams { setLong(1, id); setEnumName(2, status) }; setResultValidator { require(it == 1) } }
val id = connection.executeReturnKey(insertSql) { setParams { … } }
val n = connection.executeReturnUpdated(sql) { … }
connection.executeBatch(sql, items) { setParams { item -> setLong(1, item.id) } }
connection.queryFirstOrNull(sql) { setParams { … }; setRowMapper { row -> row.getLong("id") } }   // also queryFirst/List/Set
```
Call `setParams` once per statement; params are 1-indexed; default query timeout 30 s; `Row` has `get…`/`get…OrNull`,
`getEnum`, `getPeriode`, `getUUID`, `getArray`. On exception a transaction rolls back to the last `markerSavepoint()` and
commits the rest (see motor-jobber §3) — don't call `markerSavepoint()` casually.

## 6. Locks and versjon

`TaSkriveLåsRepository`: `lås(sakId, behandlingId)` / `låsSak` / `låsBehandling` / `withLås…` = `SELECT versjon … FOR UPDATE`
(always sak before behandling, to avoid deadlocks); `verifiserSkrivelås(lås)` = `UPDATE … SET versjon = versjon + 1 WHERE
versjon = ?` (must hit 1 row). `behandling.versjon` is what the frontend sends as `behandlingVersjon`; every processing run
bumps it, so a stale page gets `OutdatedBehandlingException`. Jobs and API writes that change a behandling take these locks.

## 7. Time zone

JVM and DB session must be `Europe/Oslo` (`verifiserTidssone` fails startup otherwise; the Gradle test task sets `TZ`).
Use `LocalDate`/`LocalDateTime` in Oslo time; `Tid.MAKS` = 2999-01-01 is "open end".

## 8. Repository tests

`repository/src/test/...`: `TestDataSource()` in `@BeforeAll`, `dataSource.transaction { c -> val sak = sak(c);
val behandling = finnEllerOpprettBehandling(c, sak) … }` (helpers in `lib-test/.../help/`), test `lagre`/`hent`/`kopier`/`slett`
including that `kopier` + `lagre` on the new behandling does not change the old one. Example: `BistandRepositoryImplTest`.
Run just that class (`python3 .agents/skills/testing/scripts/kjor_test.py BistandRepositoryImplTest`); skeleton and
running rules in the testing skill.
