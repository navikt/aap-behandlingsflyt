# lib-test — supplements the root AGENTS.md (read that too)

Shared test fixtures for every module (it is `main` code here, so changes recompile and affect tests everywhere).
Load skill `testing`; the fakes catalogue is `.agents/skills/testing/references/fakes.md`.

- Fakes over mocks: when a test needs a repository or gateway that has no fake, add the fake here instead of mocking it.
- New repository → `test/inmemoryrepo/InMemory<Name>Repository` (`object` with a `ConcurrentHashMap`; implement `kopier`
  and `slett` too) and register it in `inMemoryRepositoryRegistry`.
- New gateway → in-process `Fake<Name>Gateway` (`companion object : Factory<…>` or an `object`) and/or an HTTP fake in
  `test/fakes/`, started and wired (`INTEGRASJON_*_URL` system properties) in `FakeServers`; register the real impl in
  `testGatewayProvider()`.
- New `BehandlingsflytFeature` → add it to `LokalUnleash` in `test/FakeUnleash.kt` (it throws for unknown toggles).
- In-memory `object` repositories, `FakeServers` and `FakePersoner` are JVM-global: never rely on them being empty.
