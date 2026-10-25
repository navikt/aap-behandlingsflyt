---
applyTo: "api/src/main/kotlin/**,app/src/main/kotlin/**"
description: "Ktor-ruter og DTO-er i api og app: ingen logikk i rutefiler, transaksjon per kall, autorisasjon, montering i App.kt"
---
# Regler for api og app

- Ingen forretningslogikk i rutefiler (standup-vedtak 2025-11-10). Ruten åpner transaksjon
  (`dataSource.transaction { connection -> … }`), lager `repositoryRegistry.provider(connection)`, kaller en
  service/steg i domenet, mapper til DTO. `readOnly = true` for GET.
- Autorisasjon via tilgang-plugin: `authorizedGet/Post` med `AuthorizationParamPathConfig`/`AuthorizationBodyPathConfig`
  og riktig `Operasjon`; for løsninger sendes `avklaringsbehovKode` så `Definisjon.løsesAv` håndheves. Ingen rute uten.
- Ny rute: `fun NormalOpenAPIRoute.xxxApi(dataSource, repositoryRegistry, gatewayProvider)` i riktig feature-pakke,
  monter i `App.kt`. Endret respons-DTO endrer `openapi.json` → frontend-typer; nevn det i PR-beskrivelsen.
- Hold `DBConnection`, `*RepositoryImpl` og jobbtype-strenger ute av `api`. Ikke konstruer steg direkte i ruter
  (fem eksisterende tilfeller er gjeld, ikke mal).
- `api/.../test/` er testendepunkter som bevisst ligger i `main` (skrudd av i prod). Ikke slett, ikke utvid med prodlogikk.
- Kafka-konsumenter ligger i `api/.../hendelse/kafka/`. En uventet exception avslutter konsument-løkka; håndter
  forventede feil eksplisitt. Dette er det området som oftest får fiks-commits uten test (59 %).
- Test: Ktor `testApplication` med `MockDataSource` tåler ingen transaksjon – bruk `TestDataSource()` hvis ruten
  faktisk leser. Minst én 401-test for ny rute.
