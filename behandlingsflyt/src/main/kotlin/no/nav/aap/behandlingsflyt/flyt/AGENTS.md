# Flow engine (`flyt/`) — supplements the root AGENTS.md (read that too)

Load skill `flyt-orkestrering` before changing anything here: every flow and every open behandling in production runs on
this state machine.

Invariants the code relies on:
- Step phases: START → OPPDATER_FAKTAGRUNNLAG → UTFØRER → AVKLARINGSPUNKT → AVSLUTTER (`StegStatus.rekkefølge()`; the
  `StegOrkestrator` KDoc omits OPPDATER_FAKTAGRUNNLAG). AVKLARINGSPUNKT is the only stop point; TILBAKEFØRT is written by rewinds only.
- `StegOrkestrator` marks DB savepoints after START and OPPDATER_FAKTAGRUNNLAG (unless `markSavepointAt` is overridden);
  on failure dbconnect rolls back to the last savepoint and commits the rest.
- `BehandlingFlyt` is a stateful cursor (`aktivtSteg`); `behandling.flyt()` returns a fresh one positioned on the first step.
  Tilbakeflyt instances move their parent's cursor as they walk backwards.
- Rewinds (`tilbakefør`) run no step code and are skipped for IVERKSETTES/AVSLUTTET behandlinger.
- `prosesserBehandling` publishes `behandlingHendelseService.stoppet(...)` on every stop and at the end, but not when it exits
  because of `stoppNårStatus` (atomic runs).

Tests: `BehandlingFlytTest`, `EnklereFlytOrkestratorTest`, `FlytOrkestratorTest`, plus flow tests in `src/test/.../flyt/`.
