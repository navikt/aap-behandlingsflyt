---
applyTo: "repository/src/main/kotlin/**,lib-test/src/main/kotlin/no/nav/aap/behandlingsflyt/test/inmemoryrepo/**"
description: "Repositories: håndskrevet SQL, aktiv-flagg, kopier og slett, InMemory-variant og registrering"
---
# Regler for repositories

- Håndskrevet SQL via `DBConnection` (`queryFirstOrNull`/`queryList`/`execute*` med `setParams` og `setRowMapper`).
  Ingen ORM, ingen query builder. Parametre er posisjonelle: setter du inn en kolonne midt i en INSERT, må alle
  indekser etter flyttes.
- Grunnlag: les med `WHERE behandling_id = ? AND aktiv = TRUE`; skriv med "deaktiver eksisterende + insert ny"
  bare når det nye grunnlaget er ulikt det gamle; `kopier(fra, til)` er `INSERT … SELECT` av den aktive raden
  (vurderings-rader deles mellom behandlinger, kopieres ikke); `slett(behandlingId)` er hard sletting (GDPR) og må
  hente ider via subselect fordi radene kan være delt. Mal: `SykdomRepositoryImpl`.
- Aldri `UPDATE`/`DELETE` på grunnlags- eller vurderingsrader utenom `aktiv = false` og `slett()`.
- Ny impl: `companion object : Factory<XxxRepositoryImpl>`, registrer i `PostgresRepositoryRegistry.kt`
  (kompilerer uten, feiler i runtime), lag `InMemoryXxxRepository` og registrer i `InMemoryRepositoryRegistry.kt`.
  In-memory-varianten må implementere samme `aktiv`-semantikk, ikke "siste skriv vinner".
- Gateways: grensesnitt `XxxGateway : Gateway` i domenet, `XxxGatewayImpl` i `integrasjon/<system>/`, registrer i
  `DefaultGatewayProvider.kt` **og** `TestGatewayProvider.kt`, og lag/utvid `Fake*` i `lib-test`.
  Gateway-kode er det mest feilutsatte området i historikken (25–41 % fiks-andel): skriv test mot fake-serveren.
- Test: `XxxRepositoryImplTest` mot `TestDataSource()` (Postgres via Testcontainers) som dekker lagre/hent,
  deaktivering, `kopier` og `slett` (idempotent). Repositories er den best testede kategorien i repoet; hold nivået.
