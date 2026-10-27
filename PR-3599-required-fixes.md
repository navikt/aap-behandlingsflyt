# Required fixes before merging PR #3599

Current review target: [PR #3599](https://github.com/navikt/aap-behandlingsflyt/pull/3599), head `b1a3bd7f81195e2aab6d4bb6b232c3fd0eb327da`. The original ten-issue investigation below refers to `cd0eb0f607f21db40d3cbf027b9e7e2d1460e4d8`.

Prerequisite: fetched main `fe11f61f41` includes the migration compatibility fixes through #3711 and the migration safeguards from #3722. Deploy the explicit historical-rule-set preparation described below before enabling the write migration. Its adapter lets the existing backend read migrated assessments; it does not replace the phase-first compatibility needed by #3599.

All ten issues remain covered by regressions, including those now addressed upstream. Issue 8's migration safeguards have landed on main in #3722; this branch reuses that implementation, adapting it to the new read-only validator and preserving the explicit rule set. The historical policy agreed for this implementation preserves legacy outcomes and missing answers through an explicit persisted rule set.

## Local implementation and deployment order

| Step | Branch / target | Implemented behavior and deployment gate |
|---|---|---|
| 1 | `fix/etablering-historisk-regelsett` -> `main`; commit `776405964a` | Adds `regelsett` with database default `LEGACY`, and preserves it through repository reads, writes, copying and migration. Existing evaluation is unchanged. Existing and migrated historical rows retain their original answers, including nulls. Run and verify the write migration on this prerequisite before deploying #3599. |
| 2 | `fix/pr3599-etablering-kompatibilitet` -> `egenVirksomhetBE` (#3599) | Tracks the updated PR head through a normal merge and carries only establishment-related fixes, compatibility changes and the step 1 prerequisite. Unrelated main commits are deliberately excluded from this review branch. Covers issues 1-10 with executable regression tests. Evaluation switches explicitly on persisted `LEGACY` / `FASEBASERT`; identifiable legacy requests retain legacy policy, while new phase-based requests require the new answers. |
| 3 | Frontend rollout | Switch the frontend to phase-based requests after backend deployment. The backend assigns provenance from the input protocol, not from missing answers; clients cannot select a `regelsett`. |
| 4 | Separate cleanup | Check for legacy writes made during rollout, rerun the migration as needed, verify the data, then remove old protocol/conversion support separately. Do not remove the historical evaluation policy just because storage has been converted to phases. |

The fixes are reviewed in #3712 against `egenVirksomhetBE`. Its original main-integrated history is retained locally as `backup/pr3599-main-integration-20261026`; that version included 11 main commits absent from the target, which made the PR diff unnecessarily broad. The focused replacement excluded those unrelated changes. The update to `b1a3bd7f81` preserves the published fixes history and needs only a normal push. The previous fixes head is retained as `backup/pr3599-before-b1a3-20261027`.

Deploy step 1 before #3599 as planned. The focused review branch does not replace the final integration with main before deploying #3599. Application startup remains `dryRun = true`. Real write-mode migrations are exercised only against the test database.

**Lossless legacy writes:** issue 1 is implemented by keeping both complete legacy period lists and their original overall validity bounds in storage, then expanding every period in the shared `somFasetidslinje()` read/calculation view. Splitting only the positive periods during a legacy write would lose the assessment's broader override envelope and could resurrect older assessments in gaps or after the last submitted period. Legacy-only, phase-only and mixed histories use the same effective view.

Executable coverage is in [app/src/test/kotlin/no/nav/aap/behandlingsflyt/Pr3599ReviewTest.kt](app/src/test/kotlin/no/nav/aap/behandlingsflyt/Pr3599ReviewTest.kt), alongside the extended domain, API and [migration tests][Migration tests]. It covers the ten issues plus missing-answer validation with finite ends, format classification, functional legacy-period changes, required positive legacy periods, and lossless mixed-history save/reload. PostgreSQL tests cover successful migration/rerun, provenance/null preservation, and rollback on duration, validation-status, outcome or period changes.

The issue table and embedded diagnostic source below document the original PR-head investigation. The embedded class is frozen reproduction material for that old revision, **not a replacement for the current executable tests**. In particular, its issue 1 assertion requiring three stored rows is superseded by the lossless-storage design above; the implementation asserts all three effective phase periods instead.

Before production rollout, coordinate the historical-policy and protocol changes with sysdok. The API's top-level `tom` aggregation remains a separate frontend-contract question, not one of the ten reproduced fixes; this branch does not claim to settle it.

### Review of `b1a3bd7f81`

The public, read-only `validerVurdering` returning a nullable validation error is a useful separation from calculation. The simpler complete-history `Beregning` contract also fixes duplicate history storage in the solver. Ordinary development followed by startup now passes without an artificial negative tail; a passing control test retains that behavior.

Two regressions were reproduced on the updated upstream head:

| Regression | Reproduction and expected behavior | Local correction |
|---|---|---|
| Phase-format responses lose their periods | The existing [response test][Response tests] fails for migrated phase records because their legacy period lists are empty. Both development and startup must still appear in the response. | Retain phase-aware response conversion and effective segment bounds. |
| Startup rewrites inherited audit history | New solver test `oppstart endrer ikke lagrede datoer i arvet utviklingsvurdering`: store development through start +25 days, then submit startup at start +14 days. The saved inherited development must still end at +25, not +13. | Clip only the effective timeline and automatically generated current-input dates. Return original inherited records exactly once, plus prepared current-treatment records; do not shorten stored history. |

The upstream commit adds no tests. The fixes branch retains those two regressions and the new passing transition control. It also retains explicit finite-date preservation, legacy input compatibility and the historical-null policy that the upstream refactor does not address. Migration validation now calls the read-only API rather than recalculating stored assessments.

### Verification scope

The updated branch passes **65 related tests** across the domain, API and application modules, including the 15-test issue-focused class, both new solver tests and real PostgreSQL migration/rollback tests. It uses the PR target's own unchanged dependencies and build configuration. `detektMain` completes under the repository's configuration with existing unrelated findings; this is not a claim of a clean repository-wide static-analysis baseline.

The broader `./gradlew test detektMain` run had one failure in the unchanged [BarnFlytTest](behandlingsflyt/src/test/kotlin/no/nav/aap/behandlingsflyt/flyt/BarnFlytTest.kt): a date-dependent fixture constructs `Periode(2027-01-01, 2026-12-31)`. Main already fixes this in #3721 (`0d0c95ce50`). That unrelated change is deliberately not imported into #3712. The focused tests and `detektMain` were rerun successfully afterward. Production migration has not been run.

Scoped precommit security check:

- **Inspected:** the integration diff against `8c4caf396f`, including service/step/solver validation, phase helpers, API mapping and the migration's validation/semantic guards. The landed migration implementation was also compared with `origin/main`.
- **Not inspected:** production data/configuration, unrelated main-branch or user-owned untracked files, existing dependency vulnerabilities and a full application security audit. `trivy` and `zizmor` were unavailable; no automated vulnerability scan is claimed.
- **Findings:** 0 blocking findings and 0 concerns in the inspected source changes.
- **Verdict: CLEAN** for the inspected change only; this is not production migration approval or a full security certification.

| Axis | Evidence and limitation |
|---|---|
| SQL injection | The rule-set value remains bound with `setEnumName(19, ...)`; no new data interpolation in production SQL. |
| Secrets and PII logging | The integration diff adds no credentials or user identifiers in logs; migration errors identify treatment/ground IDs. Existing history and production logs are not audited. |
| Input validation | New inputs pass answer validation before calculation; stored records use the read-only validator, and persisted provenance still determines the historical policy. Regression tests exercise missing answers and finite dates. |
| Access control and tokens | The existing `getGrunnlag` role/resolver protection remains; no token, ownership, M2M or authentication configuration changes. Existing authentication implementation is outside this review. |
| Network policy, CORS and HTTPS | No Nais, CORS or outbound-service changes in the integration diff. Deployment configuration is not re-audited. |
| Uploads, XSS and deserialization | No upload or HTML-rendering code changes; API/domain mapping remains typed. No claim about unrelated serialization or frontend code. |
| Dependencies and automation | No dependency or workflow changes are imported. Existing vulnerabilities and automated scanner results remain unverified. |

## Confirmed issues

File names link to the affected paths. Line numbers refer to the reviewed PR revision, not the migration branch.

| # | Issue and short explanation | Proposed fix / illustrative pseudocode | Production files needing changes |
|---|---|---|---|
| 1 | **Legacy input loses periods and phases.** A request containing two development periods and one startup period saves only one startup assessment. The mapper chooses one phase and its first period. | Preserve both complete legacy lists and the original validity envelope in the mapped/stored assessment. Expand every interval in the shared effective phase view, not just the first interval. Keep explicit phase-based input as a separate mapping path. This avoids losing override coverage outside individual positive periods. | [DTO], lines 43-64; [Solver]; shared `EtableringFasetidslinje.kt`. |
| 2 | **Explicit end dates are replaced by quota-derived dates.** A legacy development period ending 9 January 2026 is saved ending 6 July. Normal validation also recalculates stored periods in memory. | Separate input preparation from read-only validation. Calculate an end date only for input identified as requiring automatic calculation; preserve explicit legacy dates and already-stored dates. For example: `prepared = prepareNewPhaseInput(input); validate(history + prepared)`. Do not infer automatic calculation solely from `fase != null`. | [Service], lines 113-129; [Step], lines 84-89; [DTO] if input provenance is needed when preparing assessments. |
| 3 | **Normal development-to-startup transitions fail.** Development receives its full quota end date, then reappears after startup ends. Validation rejects the resulting timeline with `OppstartFørUtvikling`. | Bound the generated earlier phase at the next overriding assessment: conceptually `end = min(quotaEnd, nextAssessmentStart.minusDays(1))`. Calculate quotas against the resulting effective timeline. Do not require a synthetic negative assessment after startup to prevent development from resuming. Preserve legitimate gaps and overrides. | [Service], calculation loop and phase-order validation; [Calculator], lines 17-49. |
| 4 | **Fully superseded history prevents valid corrections.** Replacing an old startup assessment with development from the same start date still validates the obsolete startup against the replacement and rejects it. | Use effective historical segments for cross-period and phase-order checks: `validateTimeline(effectiveTimeline(history + submitted))`. Validate submitted answers separately, so invalid new input cannot disappear merely because another assessment overrides it. Keep superseded records for audit, not as effective timeline constraints. | [Service], especially lines 155-157 and 189-194. |
| 5 | **Historical positive assessments become negative after migration.** Historical answers can be `null`; conversion copies them unchanged, but the PR newly requires affirmative active-work and startup-registration answers. | Agree the historical policy before implementation. Apply an explicit legacy/version-aware evaluation rule based on reliable provenance, while applying the new requirements to new assessments. Preserve missing historical answers as missing; do not fabricate that a caseworker answered yes. If existing data cannot distinguish versions reliably, agree a staged provenance solution first. | [Assessment], lines 33-45; [Service] and [DTO] for applying the agreed distinction. [Migration] only if the approved policy requires preserving additional provenance during conversion. |
| 6 | **Unmigrated or mixed-format stored data is unsupported.** A legacy record returns empty API periods, while phase-based evaluation and quota logic cannot interpret its legacy lists. Old backend instances can create such records after a migration snapshot. | Introduce a shared local-domain normalization boundary before phase-based calculations and response mapping. Expand both legacy period lists, preserve effective override semantics, and accept mixed-format history. Do not merely apply the current branch's new-to-legacy adapter in reverse or normalize repository objects into a hybrid format that can be persisted accidentally. | [Assessment] or a nearby shared domain conversion helper; [Service]; [Response], lines 73-81. Wire every phase-based evaluation/counting consumer through the same normalization. |
| 7 | **New input silently receives an affirmative answer.** `jobberBrukerAktivMedVirksomheten ?: true` also applies to explicit new-format input, allowing an unanswered question to produce a positive assessment. | Branch by input format. For new phase-based input, require the answer through normal validation or preserve `null` so it cannot count as an affirmative answer. Any legacy compatibility default must be explicitly approved and confined to identifiable legacy input. Coordinate with issue 5; missing answers alone do not identify legacy input. | [DTO], line 54; [Service] and [Assessment] for consistent missing-answer validation/evaluation. |
| 8 | **The migration rolls back despite successful before/after validation.** `Gyldig` now includes calculation data, so comparing whole results fails after assessments are split or otherwise transformed. This was reproduced with PostgreSQL. | Replace whole-result equality with an explicit semantic comparison. Compare validity/error category and separately verify the effective phase periods, weekday counts and approved outcomes remain equivalent. Conceptually: `check(sameValidationStatus(before, after)); check(sameMigrationSemantics(before, after))`. Keep duration safeguards; do not fix this by removing all checks. | [Migration], original lines 218 and 232. Safeguards landed on main in #3722; this branch reuses them with the updated read-only validation API and rule-set preservation. |
| 9 | **Historical assessments are saved twice.** The service's calculated list already contains inherited history; the solver adds the old assessments again. An unchanged revision saves two copies of one assessment. | Define whether the preparation result contains new assessments only or complete history, then assemble the persisted list exactly once. For example, a new-only result allows `save(existingHistory + preparedNewAssessments)`. Preserve superseded audit records: do not blindly persist only the filtered effective timeline or deduplicate legitimate records by structural equality. | [Service], result assembled around line 112; [Solver], lines 39-46. |
| 10 | **API periods use the original start instead of the effective segment start.** Development resuming on 17 January is returned as starting 5 January, overlapping an intervening override. | Build derived phase periods from the effective segment's bounds: `period = Periode(segment.fom, segment.tom)`, subject to the existing open-ended representation. Preserve original assessment metadata separately if needed. Test both clipped starts and clipped ends, not just unsplit assessments. | [Response], lines 58-62 and 73-80. |

## Regression coverage to retain

The original review ran 41 existing related tests successfully. Eleven additional diagnostic tests produced ten failures reproducing the issues above and one passing equality-control test on the isolated old PR checkout. The fixes branch now includes the executable, updated regressions described above; the original source remains embedded below for historical reproduction.

| Issues | Test files to extend | Minimum regression |
|---|---|---|
| 1, 2, 7, 9 | [Solver tests], [Service tests] | Preserve both phase lists and every interval; preserve a finite legacy end date; reject/preserve an unanswered new-format question; save inherited history only once across repeated revisions. |
| 3, 4 | [Service tests], [Solver tests], [Flow tests] | Accept development followed by startup without a synthetic negative tail; accept a correction that fully supersedes an old phase; continue rejecting genuinely invalid effective phase order. |
| 5, 6 | [Conversion tests], [Service tests], [Migration tests], [Response tests] | Apply the approved historical-null policy; handle legacy-only, new-only and mixed histories consistently; preserve effective overrides and quota counts. |
| 8 | [Migration tests] | Run an actual write migration with positive phase assessments and assert commit, semantic preservation and rerun safety. Cover historical/inactive grunnlag too. Identical-input equality alone does not exercise the failure. |
| 10 | [Response tests] | Override the middle of an older assessment and assert the exact two remaining intervals without overlap or coverage of the overridden interval. |

## One failing test proposal per issue

These original proposals describe the diagnostic reproductions and additional assertions for the regression suite. They are retained as issue specifications; current implementation and executable coverage are described above. All dates below are in 2026.

Use the existing in-memory repositories and fake gateways except for issue 8, which needs the real PostgreSQL transaction. Give each test a fresh treatment/history. Unless a row explicitly tests missing answers, set all required answers to affirmative values and satisfy the sickness and assistance prerequisites. This prevents unrelated validation failures from masking the intended bug. Construct historical domain objects directly where specified rather than passing them through the faulty DTO mapper.

| # | Proposed test and placement | Arrange / act | Expected assertions | Why it fails on the reviewed PR |
|---|---|---|---|---|
| 1 | `legacy payload preserves every development and startup period` in [Solver tests]. | Submit one legacy DTO with `fase = null`, development periods **5-9 January** and **19-23 January**, and startup **2-6 February**. Call the real solver with an in-memory repository and reload what it saved. | First assert `saved.hasSize(3)`. Assert two assessments have `fase = UTVIKLING` and one has `fase = OPPSTART`. After issue 2 is fixed, also compare the exact phase/period tuples with `usingRecursiveComparison()`, ignoring generated metadata only. | The mapper chooses startup because its list is nonempty and takes only that list's first period. It saves **one startup assessment**, so the count assertion exposes period loss independently of end-date calculation. |
| 2 | `legacy submission keeps the explicitly assessed end date` in [Solver tests]. | Submit only one legacy development period, **5-9 January**, with the corresponding finite top-level end date. Solve and reload the saved assessment. A single interval avoids issue 1's multiple-period loss. | `assertThat(saved.single().tom).isEqualTo(date("2026-01-09"))`. Also assert the start remains 5 January. | The service recalculates the phase to its full quota and saves **6 July** instead of **9 January**. This demonstrates a persisted behavior change, not just a different intermediate calculation. |
| 3 | `ordinary development followed by startup is accepted without artificial rejection tail` in [Service tests]. | Create phase-based development starting **5 January** and startup starting **19 January**, both with automatically calculated ends. Call `beregnOgValider`. Do **not** append a negative or phase-null tail. | Assert the result is `Gyldig`. For the completed fix, inspect the effective timeline: development must not reappear after startup, and its effective end must precede startup's start. | Full-quota development outlasts startup and resumes afterward. The service returns **`OppstartFørUtvikling`**, although the submitted phase order is development followed by startup. |
| 4 | `fully superseded startup does not block a correction to development` in [Service tests]. | Save an affirmative startup assessment for **5-9 January** in a previous treatment. In a newer treatment, submit development from **5 January** that covers the entire old startup interval. Call the service using the stored previous history. | Assert `Gyldig`. Also assert that the effective timeline has no remaining startup segment from the old assessment. Retain the old assessment as audit history. | Cross-period validation still iterates the fully overridden old startup assessment and compares it with replacement development, producing **`OppstartFørUtvikling`**. |
| 5 | `migrating historical positive assessments does not make them negative` in [Conversion tests]. | Directly construct an assessment known to satisfy the **pre-PR positive predicate**, with development **5-9 January**, startup **12-16 January**, and both active-work and registration answers `null`. Convert with `tilNyttFormat()` and evaluate each resulting assessment. | Under the proposed historical-outcome-preservation policy, assert `outcomes.containsExactly(true, true)`. Also assert the historical unanswered fields remain `null`: preserving an outcome must not fabricate answers. | Conversion preserves the missing answers, while the PR evaluator requires affirmative answers. The actual outcomes are **`[false, false]`**. This test expresses the historical compatibility policy; obtain domain agreement before making that policy authoritative. |
| 6 | `unmigrated stored assessments retain their periods in API responses` in [Response tests]. | Directly construct or load an unmigrated domain assessment with `fase = null` and development **5-9 January** in its legacy list. Map it through `EtableringEgenVirksomhetVurderingResponse.fraDomene`, using the existing fake staff-information gateways. Do not convert it in the test first. | `assertThat(response.utviklingsPeriode).containsExactly(Periode(5 Jan, 9 Jan))`. Compare the full expected response recursively where appropriate. Add sibling mixed-history quota/evaluation tests after the minimal reproduction. | Response mapping derives periods only from `fase`, so the actual development list is **empty**, despite a populated stored legacy period. This isolates missing read compatibility from DTO input loss. |
| 7 | `new format does not fabricate an affirmative active work answer` in a focused DTO test, or initially [Solver tests]. | Create explicit new-format input with `fase = UTVIKLING`, all other answers affirmative, and `jobberBrukerAktivMedVirksomheten = null`. Invoke the DTO-to-domain mapper directly. | For a preserve-then-validate implementation, assert the mapped answer `isNull()` and `evaluerVirksomhetVurdering()` is false. If the agreed contract rejects missing answers at the boundary instead, assert that specific validation error in the request-path test; do not accept any arbitrary exception as success. | The mapper substitutes **`true`**, so evaluation is **positive**. Unlike issue 5, this is an unanswered **new** assessment, not a historical assessment whose outcome needs compatibility protection. |
| 8 | `database migration commits when both validation results are successful` in [Migration tests]. | In PostgreSQL, commit the treatment prerequisites and a positive legacy assessment with development **5-9 January**. Assert both the original and prospective converted inputs validate as `Gyldig`. In a separate transaction, run the actual write migration with `dryRun = false`. | Assert migration returns without an exception. Reload in a **new transaction** and assert converted records exist and preserve the approved effective periods, weekday counts and outcomes. Rerun and assert unchanged persisted results. Use semantic projections for recursive comparison, not raw legacy/new row equality. | The migration throws **`Valideringsresultat endret ved migrering ...`** because the two successful `Gyldig(beregning)` objects contain different assessment payloads. Its transaction rolls back. Merely comparing two calls with identical input would pass and miss the bug. |
| 9 | `revurdering carries previous assessments forward exactly once` in [Solver tests]. | Save one finite development assessment, **5-9 January**, on the previous treatment. Create a reassessment with no new assessments and solve it with an empty submitted list. Reload the new treatment's saved history. | `assertThat(saved).containsExactly(previousAssessment)`. A follow-up test should repeat another reassessment and retain a count of one. Compare relevant persisted fields recursively if the repository assigns new storage identifiers. | The service includes the previous assessment in its result and the solver adds previous history again. The new treatment contains **two copies**, even though the request added nothing. |
| 10 | `API phase periods use effective segment start when an older assessment resumes` in [Response tests]. | Create older development **5-30 January**. Overlay it in a newer treatment with a finite, negative, `fase = null` assessment for **12-16 January**. Compute `gjeldendeVurderinger()` and map that effective timeline to responses. | Flatten the returned development periods and assert **exactly** `[5-11 January, 17-30 January]`. Assert neither returned interval overlaps the override. Exercise the timeline mapper, not only the single-assessment mapper. | The resumed segment uses the old assessment's original `fom`. Actual periods are **`[5-11 January, 5-30 January]`**, incorrectly including the overridden dates and overlapping the first segment. |

`date(...)`, abbreviated dates and result projections above are illustrative pseudocode. Prefer ordinary JUnit/AssertJ assertions and the project's existing builders when implementing them. A successful end state requires all ten regressions to pass together: for example, exact-period assertions in issue 1 also depend on resolving issue 2. Keep the minimal first assertion for each issue so a failure identifies the underlying regression clearly.

## Complete diagnostic tests

The full test class used for the review is embedded below, including imports and fixtures. It contains **ten failing reproductions**, matched to the table by their test names, plus one passing identical-input equality control. No test source outside this document is needed.

To reproduce on the reviewed **#3599 revision**, copy this block to `app/src/test/kotlin/no/nav/aap/behandlingsflyt/Pr3599ReviewTest.kt` and run `./gradlew :app:test --tests 'no.nav.aap.behandlingsflyt.Pr3599ReviewTest'`. The `app` module gives the diagnostic class access to the service, API mapper and PostgreSQL migration together. Docker is required for the database test. These tests target the PR's APIs, not the migration branch's older APIs.

The assertions below are the original minimal reproductions, rather than all the additional post-fix checks proposed above. In particular, issue 5 asserts the proposed historical-outcome-preservation policy, which still needs domain agreement. Issue 8 deliberately exercises the actual migration transaction; its current exception fails the test without an explicit `assertDoesNotThrow`.

```kotlin
package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.EtableringEgenVirksomhetLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.EtableringEgenVirksomhetLøsning
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.EtableringEgenVirksomhetService
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.VirksomhetEtableringResultat
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.EtableringEgenVirksomhetVurderingResponse
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.tilNyttFormat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.tilGammeltFormat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.help.avklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgRevurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBistandRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryEtableringEgenVirksomRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.FakeAnsattInfoGateway
import no.nav.aap.behandlingsflyt.test.FakeEnhetGateway
import no.nav.aap.bistandsbehov.Bistandsvurdering
import no.nav.aap.bistandsbehov.BistandRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class Pr3599ReviewTest {
    private val service = EtableringEgenVirksomhetService(
        InMemoryEtableringEgenVirksomRepository,
        InMemoryBehandlingRepository,
        InMemoryBistandRepository,
        InMemorySykdomRepository,
    )
    private val løser = EtableringEgenVirksomhetLøser(
        InMemoryEtableringEgenVirksomRepository, InMemoryBehandlingRepository, service,
    )
    private val start = LocalDate.of(2026, 1, 5)

    @Test
    fun `unmigrated stored assessments retain their periods in API responses`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(start.minusDays(4))
        val gammel = dto(fase = null)
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
            .copy(utviklingsPerioder = listOf(Periode(start, start.plusDays(4))))
        val response = EtableringEgenVirksomhetVurderingResponse.fraDomene(
            gammel,
            VurdertAvService(
                InMemoryAvklaringsbehovRepository,
                AnsattInfoService(FakeAnsattInfoGateway(), FakeEnhetGateway()),
            )
        )
        assertThat(response.utviklingsPeriode).isEqualTo(gammel.utviklingsPerioder)
    }

    @Test
    fun `validation result equality remains stable for identical input`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(start.minusDays(4))
        oppfyllSykdomOgBistand(behandling.id)
        val vurderinger = listOf(dto().toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id))
        assertThat(service.beregnOgValider(behandling.id, vurderinger))
            .isEqualTo(service.beregnOgValider(behandling.id, vurderinger))
    }

    @Test
    fun `new format does not fabricate an affirmative active work answer`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(start.minusDays(4))
        val vurdering = dto().copy(jobberBrukerAktivMedVirksomheten = null)
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
        assertThat(vurdering.evaluerVirksomhetVurdering()).isFalse()
    }

    @Test
    fun `database migration commits when both validation results are successful`() {
        TestDataSource().use { dataSource ->
            val behandlingId = dataSource.transaction { connection ->
                val behandlingId = finnEllerOpprettBehandling(connection, sak(connection)).id
                oppfyllSykdomOgBistand(behandlingId)
                val provider = postgresRepositoryRegistry.provider(connection)
                provider.provide<SykdomRepository>().lagre(
                    behandlingId, requireNotNull(InMemorySykdomRepository.hentHvisEksisterer(behandlingId)).sykdomsvurderinger
                )
                provider.provide<BistandRepository>().lagre(
                    behandlingId, requireNotNull(InMemoryBistandRepository.hentHvisEksisterer(behandlingId)).vurderinger
                )
                val gammel = dto(fase = null)
                    .toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandlingId)
                    .copy(utviklingsPerioder = listOf(Periode(start, start.plusDays(4))))
                provider.provide<EtableringEgenVirksomhetRepository>().lagre(behandlingId, listOf(gammel))
                assertThat(EtableringEgenVirksomhetService(provider).beregnOgValider(behandlingId, listOf(gammel)))
                    .isInstanceOf(VirksomhetEtableringResultat.Gyldig::class.java)
                assertThat(
                    EtableringEgenVirksomhetService(provider).beregnOgValider(
                        behandlingId, listOf(gammel).tilNyttFormat().map { it.tilGammeltFormat() }
                    )
                ).isInstanceOf(VirksomhetEtableringResultat.Gyldig::class.java)
                behandlingId
            }
            dataSource.transaction { connection ->
                BackfillEtableringVurderinger(dataSource, dryRun = false).migrerBehandling(connection, behandlingId)
            }
        }
    }

    @Test
    fun `migrating historical positive assessments does not make them negative`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(start.minusDays(4))
        val gammel = dto(fase = null)
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
            .copy(
                utviklingsPerioder = listOf(Periode(start, start.plusDays(4))),
                oppstartsPerioder = listOf(Periode(start.plusDays(7), start.plusDays(11))),
                jobberBrukerAktivMedVirksomheten = null,
                erRegistrertINødvendigeOffentligeRegister = null,
            )
        assertThat(listOf(gammel).tilNyttFormat().map { it.evaluerVirksomhetVurdering() })
            .containsExactly(true, true)
    }

    @Test
    fun `API phase periods use effective segment start when an older assessment resumes`() {
        val (_, forrige, behandling) = opprettInMemorySakOgRevurdering(start.minusDays(4))
        val eldre = dto(tom = start.plusDays(25))
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), forrige.id)
        val nyere = dto(start.plusDays(7), start.plusDays(11), fase = null)
            .copy(virksomhetErNy = false)
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
        val response = EtableringEgenVirksomhetVurderingResponse.fraDomene(
            listOf(eldre, nyere).gjeldendeVurderinger(),
            VurdertAvService(
                InMemoryAvklaringsbehovRepository,
                AnsattInfoService(FakeAnsattInfoGateway(), FakeEnhetGateway()),
            )
        )
        assertThat(response.flatMap { it.utviklingsPeriode.orEmpty() }).containsExactly(
            Periode(start, start.plusDays(6)),
            Periode(start.plusDays(12), start.plusDays(25)),
        )
    }

    @Test
    fun `fully superseded startup does not block a correction to development`() {
        val (_, forrige, behandling) = opprettInMemorySakOgRevurdering(start.minusDays(4))
        oppfyllSykdomOgBistand(behandling.id)
        val tidligere = dto(tom = start.plusDays(4), fase = EtableringFase.OPPSTART)
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), forrige.id)
        InMemoryEtableringEgenVirksomRepository.lagre(forrige.id, listOf(tidligere))
        val ny = dto().toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id)
        assertThat(service.beregnOgValider(behandling.id, listOf(ny)))
            .isInstanceOf(VirksomhetEtableringResultat.Gyldig::class.java)
    }

    @Test
    fun `revurdering carries previous assessments forward exactly once`() {
        val (_, forrige, behandling) = opprettInMemorySakOgRevurdering(start.minusDays(4))
        oppfyllSykdomOgBistand(behandling.id)
        val tidligere = dto(tom = start.plusDays(4))
            .toEtableringEgenVirksomhetVurdering(Bruker("saks"), forrige.id)
        InMemoryEtableringEgenVirksomRepository.lagre(forrige.id, listOf(tidligere))
        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            EtableringEgenVirksomhetLøsning(emptyList()),
        )
        assertThat(
            InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)?.vurderinger
        ).containsExactly(tidligere)
    }

    @Test
    fun `legacy submission keeps the explicitly assessed end date`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(start.minusDays(4))
        oppfyllSykdomOgBistand(behandling.id)
        val tom = start.plusDays(4)
        val input = dto(tom = tom).copy(
            fase = null, utviklingsPerioder = listOf(Periode(start, tom))
        )
        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            EtableringEgenVirksomhetLøsning(listOf(input)),
        )
        val lagret = requireNotNull(
            InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)
        ).vurderinger.single()
        assertThat(lagret.tom).isEqualTo(tom)
    }

    @Test
    fun `ordinary development followed by startup is accepted without artificial rejection tail`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(start.minusDays(4))
        oppfyllSykdomOgBistand(behandling.id)
        val vurderinger = listOf(dto(), dto(start.plusDays(14), fase = EtableringFase.OPPSTART))
            .map { it.toEtableringEgenVirksomhetVurdering(Bruker("saks"), behandling.id) }
        assertThat(service.beregnOgValider(behandling.id, vurderinger))
            .isInstanceOf(VirksomhetEtableringResultat.Gyldig::class.java)
    }

    @Test
    fun `legacy payload preserves every development and startup period`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(start.minusDays(4))
        oppfyllSykdomOgBistand(behandling.id)
        val utvikling = listOf(
            Periode(start, start.plusDays(4)),
            Periode(start.plusDays(14), start.plusDays(18)),
        )
        val oppstart = listOf(Periode(start.plusDays(28), start.plusDays(32)))
        val input = dto().copy(
            fase = null,
            utviklingsPerioder = utvikling,
            oppstartsPerioder = oppstart,
        )
        løser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            EtableringEgenVirksomhetLøsning(listOf(input)),
        )
        val lagret = requireNotNull(
            InMemoryEtableringEgenVirksomRepository.hentHvisEksisterer(behandling.id)
        ).vurderinger
        assertThat(lagret).hasSize(3)
        assertThat(lagret.map { Periode(it.fom, requireNotNull(it.tom)) })
            .containsExactlyInAnyOrderElementsOf(utvikling + oppstart)
    }

    private fun dto(
        fom: LocalDate = start,
        tom: LocalDate? = null,
        fase: EtableringFase? = EtableringFase.UTVIKLING,
    ) = EtableringEgenVirksomhetLøsningDto(
        begrunnelse = "Review reproduction",
        fom = fom,
        tom = tom,
        virksomhetNavn = "Testvirksomhet",
        foreliggerFagligVurdering = true,
        virksomhetErNy = true,
        brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
        kanFøreTilSelvforsørget = true,
        fase = fase,
        erRegistrertINødvendigeOffentligeRegister = true,
        jobberBrukerAktivMedVirksomheten = true,
    )

    private fun oppfyllSykdomOgBistand(behandlingId: BehandlingId) {
        val fom = start.minusDays(4)
        InMemorySykdomRepository.lagre(
            behandlingId, listOf(
                Sykdomsvurdering(
                    begrunnelse = "Review reproduction",
                    vurderingenGjelderFra = fom,
                    vurderingenGjelderTil = null,
                    harSkadeSykdomEllerLyte = true,
                    harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = false,
                    yrkesskadeBegrunnelse = null,
                    diagnose = null,
                    vurdertIBehandling = behandlingId,
                    opprettet = Instant.now(),
                    vurdertAv = Bruker("saks"),
                )
            )
        )
        InMemoryBistandRepository.lagre(
            behandlingId, listOf(
                Bistandsvurdering(
                    begrunnelse = "Review reproduction",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = true,
                    erBehovForAnnenOppfølging = false,
                    overgangBegrunnelse = null,
                    skalVurdereAapIOvergangTilArbeid = false,
                    vurdertAv = Bruker("saks"),
                    fom = fom,
                    tom = null,
                    opprettet = Instant.now(),
                    vurdertIBehandling = behandlingId,
                )
            )
        )
    }
}
```

## Deployment ownership and remaining decisions

| Stage / concern | Required action |
|---|---|
| Migration branch | Deploy its compatibility bridge first. `App.kt` currently starts the backfill with `dryRun = true`; deliberately enable the write migration and verify completion before deploying #3599. Review execution safety and production volume separately from the PR's correctness fixes. |
| #3599 | Implement the ten fixes above. Keep legacy reads and legacy frontend writes supported through the rolling deployment and frontend transition. Migration completion is a snapshot, not proof that legacy rows cannot reappear. |
| Historical policy | Agree issue 5 before deploying the new evaluator. Coordinate any conversion/provenance requirements with the migration before running it. |
| Response contract | Clarify `tom` in [Grunnlag API], around line 72: choosing the latest-starting raw assessment, including `fase = null`, is not necessarily the end of the effective phase timeline. Confirm the frontend contract and add a test. This is a contract follow-up, not an eleventh reproduced failure. |
| Frontend and cleanup | Deploy the new frontend after the corrected backend. Remove legacy handling only after legacy writers are gone, remaining legacy data has been checked, and the rollback window has closed. |
| PR description and sysdok | Correct the claim that a legacy repository mapper already exists in the reviewed PR. Agree whether sysdok needs updating for changed assessment rules, historical compatibility and rollout sequencing. |

## File references

Production paths below are relative to the repository root. `TilOgMedBeregning.kt` exists in the reviewed PR, not on the migration branch, so its link is pinned to the reviewed revision.

| Reference used above | File |
|---|---|
| DTO | [EtableringEgenVirksomhetLøsningDto.kt][DTO] |
| Service | [EtableringEgenVirksomhetService.kt][Service] |
| Solver | [EtableringEgenVirksomhetLøser.kt][Solver] |
| Step | [EtableringEgenVirksomhetSteg.kt][Step] |
| Calculator | [TilOgMedBeregning.kt][Calculator] |
| Assessment | [EtableringEgenVirksomhetVurdering.kt][Assessment] |
| Response | [EtableringEgenVirksomhetGrunnlagResponse.kt][Response] |
| Migration | [BackfillEtableringVurderinger.kt][Migration] |
| Grunnlag API | [EtableringEgenVirksomhetGrunnlagApi.kt][Grunnlag API] |
| Solver tests | [EtableringEgenVirksomhetLøserTest.kt][Solver tests] |
| Service tests | [EtableringEgenVirksomhetServiceTest.kt][Service tests] |
| Flow tests | [EtableringEgenVirksomhetFlytTest.kt][Flow tests] |
| Conversion tests | [TilNyttFormatTest.kt][Conversion tests] |
| Migration tests | [BackfillEtableringVurderingerTest.kt][Migration tests] |
| Response tests | [EtableringEgenVirksomhetVurderingResponseTest.kt][Response tests] |

[DTO]: behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/faktagrunnlag/saksbehandler/etableringegenvirksomhet/EtableringEgenVirksomhetLøsningDto.kt
[Service]: behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/behandling/etableringegenvirksomhet/EtableringEgenVirksomhetService.kt
[Solver]: behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/behandling/avklaringsbehov/løser/EtableringEgenVirksomhetLøser.kt
[Step]: behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/forretningsflyt/steg/EtableringEgenVirksomhetSteg.kt
[Calculator]: https://github.com/navikt/aap-behandlingsflyt/blob/cd0eb0f607f21db40d3cbf027b9e7e2d1460e4d8/behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/faktagrunnlag/saksbehandler/etableringegenvirksomhet/TilOgMedBeregning.kt
[Assessment]: behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/faktagrunnlag/saksbehandler/etableringegenvirksomhet/EtableringEgenVirksomhetVurdering.kt
[Response]: api/src/main/kotlin/no/nav/aap/behandlingsflyt/behandling/etableringegenvirksomhet/EtableringEgenVirksomhetGrunnlagResponse.kt
[Migration]: app/src/main/kotlin/no/nav/aap/behandlingsflyt/BackfillEtableringVurderinger.kt
[Grunnlag API]: api/src/main/kotlin/no/nav/aap/behandlingsflyt/behandling/etableringegenvirksomhet/EtableringEgenVirksomhetGrunnlagApi.kt
[Solver tests]: behandlingsflyt/src/test/kotlin/no/nav/aap/behandlingsflyt/behandling/avklaringsbehov/løser/EtableringEgenVirksomhetLøserTest.kt
[Service tests]: api/src/test/kotlin/no/nav/aap/behandlingsflyt/behandling/etableringegenvirksomhet/EtableringEgenVirksomhetServiceTest.kt
[Flow tests]: behandlingsflyt/src/test/kotlin/no/nav/aap/behandlingsflyt/flyt/EtableringEgenVirksomhetFlytTest.kt
[Conversion tests]: behandlingsflyt/src/test/kotlin/no/nav/aap/behandlingsflyt/faktagrunnlag/saksbehandler/etableringegenvirksomhet/TilNyttFormatTest.kt
[Migration tests]: app/src/test/kotlin/no/nav/aap/behandlingsflyt/BackfillEtableringVurderingerTest.kt
[Response tests]: api/src/test/kotlin/no/nav/aap/behandlingsflyt/behandling/etableringegenvirksomhet/EtableringEgenVirksomhetVurderingResponseTest.kt
