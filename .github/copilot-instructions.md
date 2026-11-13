# Copilot instructions

The project guidance for AI agents lives in [`AGENTS.md`](../AGENTS.md) at the repository root — read it before working.
Task-specific skills (with scripts and references) are in [`.agents/skills/`](../.agents/skills/); each `SKILL.md` says
when to use it. Nested `AGENTS.md` files exist in `kontrakt/`, `repository/`, `lib-test/`, the `flyt/` and
`forretningsflyt/` packages of the `behandlingsflyt` module, and its flyt-test package.

Tests: always `./gradlew` (never a global `gradle`), never the whole suite — one class or a small batch per run
(see the `testing` skill).
