# Flyt-tests — supplements the root AGENTS.md (read that too)

Load skill `testing`; DSL, snapshot tests and pitfalls are in `.agents/skills/testing/references/flyttester.md`.

- Classes extending `AbstraktFlytOrkestratorTest`/`AbstraktFlytOrkestratorSnapshotTest` start Postgres, all fakes and
  the motor; one class takes from seconds to over a minute. Run one class (at most ~3 per Gradle run), never
  `--tests '*FlytTest'`: `python3 .agents/skills/testing/scripts/kjor_test.py <Klasse> [-m "del av navn"]`.
  Not every class in this package is a flyt-test; `kjor_test.py --liste flyt` lists the real ones.
- Chain on the `Behandling` each DSL helper returns. A new avklaringsbehov gets a `løsX()` helper in
  `AbstraktFlytOrkestratorTest` (`kjor_test.py --dsl` lists the existing ones).
- A flyt-test failing oddly usually hides a swallowed job exception: `kjor_test.py --resultat behandlingsflyt`.
- Prefer extending an existing class; use the snapshot base class for many tests that start from the same finished
  førstegangsbehandling.
