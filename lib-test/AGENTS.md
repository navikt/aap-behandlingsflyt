# lib-test — tillegg til AGENTS.md i rota (les den også)

Delte testoppsett for alle moduler. Dette er `main`-kode, så endringer kompileres på nytt og påvirker tester overalt.
Last inn skillen `testing`. Oversikten over fakes ligger i `.agents/skills/testing/references/fakes.md`.

- Bruk fakes framfor mocks: Hvis en test trenger et repository eller en gateway uten fake, legg til en fake her i stedet for å mocke den.
- Nytt repository → `test/inmemoryrepo/InMemory<Name>Repository` (`object` med en `ConcurrentHashMap`; implementer også `kopier`
  og `slett`), og registrer det i `inMemoryRepositoryRegistry`.
- Ny gateway → `Fake<Name>Gateway` i samme prosess (`companion object : Factory<…>` eller et `object`) og/eller en HTTP-fake i
  `test/fakes/`. Start og koble den til (`INTEGRASJON_*_URL`-systemegenskaper) i `FakeServers`, og registrer den ekte implementasjonen i
  `testGatewayProvider()`.
- Ny `BehandlingsflytFeature` → legg den til i `LokalUnleash` i `test/FakeUnleash.kt` (den kaster feil for ukjente toggler).
- In-memory-`object`-repositories, `FakeServers` og `FakePersoner` er globale for JVM-en. Ikke gå ut fra at de er tomme.
