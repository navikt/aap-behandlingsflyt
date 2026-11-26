# Regler for api og app

Gjelder: `api/src/main/kotlin/**`, `app/src/main/kotlin/**`

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
  `KafkaKonsument` eier polling, iterasjon, commit og nedstenging. Den committer først når alle meldingene fra
  ett poll er håndtert. Meldingshåndtererne implementerer `KafkaMeldingsHåndterer<K, V>` med `val navn` og
  `håndter(ConsumerRecord<K, V>)`, og sendes direkte til `håndter` i `App.kt`, uten arv fra konsumenten.
  Behold navnene ved refaktorering, siden de inngår i Kafka-klient-ID-en. Test meldingshåndtereren direkte;
  bruk `MockConsumer` for konsument-løkka uten Kafka-server.
- Test: Ktor `testApplication` med `MockDataSource` tåler ingen transaksjon – bruk `TestDataSource()` (og
  `@Tag("db")` på testklassen) hvis ruten faktisk leser. Minst én 401-test for ny rute.
