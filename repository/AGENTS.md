# repository — tillegg til AGENTS.md i rota (les den også)

Last inn skillen `repository-og-migrering` før du endrer SQL, tabeller, repositories eller gatewayer.

- Flyway-migreringer: `src/main/resources/flyway/V1.<n>__<beskrivelse>.sql`. Neste nummer:
  `python3 .agents/skills/repository-og-migrering/scripts/repo.py neste-migrering --navn <beskrivelse>`. Ikke endre en merg’et
  migrering. Skriv migreringer som fungerer mens forrige app-versjon fortsatt kjører (rolling deploy, flere pods).
- Ny repository-implementasjon: `companion object : Factory<…>`, registrer i `PostgresRepositoryRegistry.kt`, implementer `kopier`
  (grunt: ny grunnlagsrad som peker på de samme underordnede radene) og en idempotent `slett`. Kontroller koblingen med `repo.py sjekk <Navn>`.
- Underordnede grunnlagsrader deles mellom behandlinger etter `kopier`: sett inn nye rader, ikke kjør UPDATE på eksisterende.
- Ny gateway-implementasjon (`integrasjon/`): registrer i `defaultGatewayProvider()` og i lib-testens `testGatewayProvider()`, legg til miljøvariabler
  i `.nais/dev.yaml` / `.nais/prod.yaml` og en fake i lib-testens `FakeServers`.
