# repository — supplements the root AGENTS.md (read that too)

Load skill `repository-og-migrering` before changing SQL, tables, repositories or gateways.

- Flyway migrations: `src/main/resources/flyway/V1.<n>__<beskrivelse>.sql`. Next number:
  `python3 .agents/skills/repository-og-migrering/scripts/repo.py neste-migrering --navn <beskrivelse>`. Never edit a merged
  migration; write migrations that work while the previous app version still runs (rolling deploy, several pods).
- New repository impl: `companion object : Factory<…>`, register in `PostgresRepositoryRegistry.kt`, implement `kopier`
  (shallow: new grunnlag row pointing at the same child rows) and an idempotent `slett`. Check wiring with `repo.py sjekk <Navn>`.
- Grunnlag child rows are shared between behandlinger after `kopier`: insert new rows, never UPDATE them in place.
- New gateway impl (`integrasjon/`): register in `defaultGatewayProvider()` and in lib-test `testGatewayProvider()`, add env
  vars to `.nais/dev.yaml` / `.nais/prod.yaml` and a fake in lib-test `FakeServers`.
