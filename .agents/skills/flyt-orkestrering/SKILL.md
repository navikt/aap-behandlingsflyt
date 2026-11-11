---
name: flyt-orkestrering
description: How the behandlingsflyt state machine actually works — FlytOrkestrator, StegOrkestrator, BehandlingFlyt, StegStatus phases, when steps run/stop/re-run, tilbakeføring (rewind), informasjonskrav, vurderingsbehov/VurderingType, atomic (fasttrack) behandlinger and savepoint failure semantics. Use for any question like "when/why does steg X run", "why did the behandling go back/stop", when changing a flow or step order, or when writing step logic. Keywords — flyt, steg, prosesser, forbered, tilbakefør, orkestrator, stegstatus, avklaringspunkt.
---

# Flow engine: FlytOrkestrator / StegOrkestrator / BehandlingFlyt

Source (≈1000 lines, read when in doubt): `behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/`
`flyt/FlytOrkestrator.kt`, `flyt/steg/StegOrkestrator.kt`, `flyt/BehandlingFlyt.kt`,
`periodisering/FlytKontekstMedPeriodeService.kt`, `faktagrunnlag/InformasjonskravGrunnlagImpl.kt`.

Tools (no build needed):
- `python3 .agents/skills/flyt-orkestrering/scripts/flyt.py --flyt Revurdering [--vb]` ordered steps, class, status, informasjonskrav, avklaringsbehov per step
- `flyt.py --steg AVKLAR_SYKDOM` (or class name) · `flyt.py --vurderingsbehov X` · `flyt.py --vurderingtype`
- [references/flyter.md](references/flyter.md) — generated tables of every flow + vurderingsbehov rewind targets
- [references/sekvenser.md](references/sekvenser.md) — step-by-step traces of the 7 main scenarios (søknad, løs behov, retur fra beslutter, nytt dokument, meldekort/fasttrack, vent, feil)

## 1. Moving parts

| Piece | Role |
|---|---|
| `BehandlingFlyt` | Immutable ordered list of `Behandlingsflytsteg(steg, kravliste, oppdaterFaktagrunnlag)` + a mutable cursor `aktivtSteg`. Built in `forretningsflyt/behandlingstyper/*.kt` with `BehandlingFlytBuilder`. `TypeBehandling.flyt()` maps type → flow. |
| `FlytOrkestrator` | Drives ONE behandling: `forberedBehandling` (sync with the world, maybe rewind) then `prosesserBehandling` (loop over steps). |
| `StegOrkestrator` | Runs ONE step through its `StegStatus` phases, persisting each phase to `steg_historikk`. |
| `BehandlingSteg` / `FlytSteg` | Step logic (`utfør(kontekst): StegResultat`) / its `companion object` factory + `StegType`. |
| `FlytKontekstMedPeriodeService` | Builds `FlytKontekstMedPerioder` per step: `vurderingType`, relevant vurderingsbehov, current `rettighetsperiode`. |
| `InformasjonskravGrunnlagImpl` | Refreshes informasjonskrav (register data / documents) and reports which ones changed. |
| `AvklaringsbehovOrkestrator` | Entry point for solving avklaringsbehov from the API (outside the loop). |
| `ProsesserBehandlingJobbUtfører` / `ProsesserBehandlingService` | Motor job `flyt.prosesserBehandling` that runs the engine / service that enqueues it or runs atomic behandlinger. |

`Førstegangsbehandling.flyt()` returns `Revurdering.flyt()` — 56 identical steps. Differences come from `kontekst.behandlingType`, `kontekst.vurderingType` and `forrigeBehandlingId`.

## 2. What triggers the engine

The engine never runs by itself. Entry points:
1. Motor job `flyt.prosesserBehandling` → `forberedOgProsesserBehandling(behandlingId)` under a sak+behandling row lock (`TaSkriveLåsRepository.lås`). Enqueued by `ProsesserBehandlingService.triggProsesserBehandling`, which is a **no-op if a KLAR or FEILET job of that type already exists for the behandling**. Callers: document/event handling, after solving an avklaringsbehov, `batch.gjenopptaBehandlinger` (daily 07:00, expired frister), drift API, after atomic behandlinger.
2. Synchronous atomic run (`ProsesserBehandlingService.kjørAtomærBehandling`, §8) inside the transaction of whatever created the behandling.
3. Drift (`Driftfunksjoner.prosesserBehandling` / `kjørFraSteg`, `/api/drift/behandling/{ref}/...`).

Solving an avklaringsbehov via `POST /api/behandling/løs-behov` does NOT run the loop: it rewinds if needed, runs the løser, marks the behov AVSLUTTET, enqueues the job and returns 202. The frontend polls `GET /api/behandling/{ref}/flyt/prosessering` (JOBBER/FEILET/FERDIG from pending jobs).

## 3. One run = forberedBehandling + prosesserBehandling

`forberedBehandling` (in order):
1. `validerTilstand` — throws `UgyldigForespørselException` if the behandling is AVSLUTTET.
2. First run ever (no steg_historikk): sak status → UTREDES.
3. Open ventebehov? `VentebehovEvaluererService` resolves those with frist ≤ today, or whose condition is met (`LegeerklæringVentebehovEvaluerer` for BESTILL_LEGEERKLÆRING). If none remain open → rewind to the step of the resolved ones.
4. Behandling status IVERKSETTES/AVSLUTTET → **stop here** (no informasjonskrav, no rewinds).
5. Earliest *måLøses* avklaringsbehov whose step is before the active step → rewind there (expected after retur from beslutter/kvalitetssikrer; otherwise logs ERROR "...har passert det åpne avklaringsbehovet... bug" — typically a step-order change).
6. Refresh informasjonskrav of all steps **strictly before** the active step (`alleFaktagrunnlagFørGjeldendeSteg`; empty when the active step is after `sluttÅOppdatereFaktagrunnlag()`).
7. Rewind to the earliest of (a) a step owning an informasjonskrav that returned ENDRET, (b) a step registered for a vurderingsbehov whose `oppdatertTid` is after that step's latest steg_historikk row. Steps with status OPPRETTET (START_BEHANDLING…) are never targets of (a)/(b).

`prosesserBehandling` (simplified from the source):
```
gjeldende = aktivt steg            // or the NEXT step if aktivt steg's last status is AVSLUTTER
behandling.status := gjeldende.type().status
loop:
  if gjeldende.status in stoppNårStatus: return          // only atomic runs set this; NO stoppet-hendelse
  kontekst = FlytKontekstMedPeriodeService.utled(..., gjeldende)
  resultat = StegOrkestrator.utfør(gjeldende, ...)        // §4
  validerPlassering: open måLøses avklaringsbehov with løsesISteg BEFORE gjeldende → IllegalStateException
  neste = if (resultat stopped) gjeldende else flyt.neste()
  behandling.status := neste.status (if changed)
  if stopped or neste == null:
     if neste == null: lukkBehandling() → AVSLUTTET (requires last phase AVSLUTTER, no måLøses behov)
     behandlingHendelseService.stoppet(...)              // jobs: oppgave, statistikk, datadeling, meldekort
     return
  gjeldende = neste
```
Behandling status is derived from `StegType.status` of the active step (OPPRETTET → UTREDES → IVERKSETTES), AVSLUTTET only via `lukkBehandling`.

## 4. Inside one step: StegStatus phases

`StegStatus.rekkefølge()` = START → OPPDATER_FAKTAGRUNNLAG → UTFØRER → AVKLARINGSPUNKT → AVSLUTTER. (The KDoc on `StegOrkestrator` omits OPPDATER_FAKTAGRUNNLAG — the enum is the truth.)

| Phase | What happens | Can stop? |
|---|---|---|
| START | marker; **savepoint** | no |
| OPPDATER_FAKTAGRUNNLAG | refresh informasjonskrav registered ON this step; result ignored (no rewind); **savepoint** | no |
| UTFØRER | `BehandlingSteg.utfør(kontekst)`; `FantVentebehov` adds the ventebehov (funnetISteg = this step) and continues | no |
| AVKLARINGSPUNKT | stop if any avklaringsbehov `skalStoppeHer(thisStep)`: `løsesISteg == thisStep` (or `funnetISteg` if UDEFINERT) **and** `måLøses()` | **yes — the only stop point** |
| AVSLUTTER | marker | no |

- Every phase inserts a `steg_historikk` row (only the newest row per behandling has `aktiv = true`).
- A behandling stopped at AVKLARINGSPUNKT restarts **the same step from START** on the next run → `utfør` runs again.
- Steps cannot stop the flow directly. `Fullført` just means "utfør finished"; the stop decision is made from avklaringsbehov in AVKLARINGSPUNKT. A step that creates a behov for a *later* step continues; for an *earlier* step → `validerPlassering` throws.
- `TILBAKEFØRT` is not a phase; it is written by rewinds.

## 5. Tilbakeføring (rewind): what it is and is not

`FlytOrkestrator.tilbakefør` walks backwards from the active step to the target (inclusive), writing a TILBAKEFØRT row per step. **It runs no step code, deletes nothing, and does not touch avklaringsbehov or grunnlag.** Data is reconciled when the steps run forward again (`AvklaringsbehovService` re-evaluates every behov: avbryt + `tilbakestillGrunnlag`, reopen, close). Rewinds are skipped for IVERKSETTES/AVSLUTTET behandlinger (warn "Prøvde å tilbakeføre avsluttet eller iverksatt behandling").

| Trigger | Where | Target |
|---|---|---|
| ventebehov resolved (frist passed / document arrived) | forbered §3.3 | step where it was waiting |
| open behov earlier than active step (SENDT_TILBAKE_FRA_BESLUTTER/_KVALITETSSIKRER, reopened) | forbered §3.5 | earliest such step |
| informasjonskrav ENDRET for a step before the active step | forbered §3.7a | earliest owning step |
| vurderingsbehov added/renewed after a registered step last ran | forbered §3.7b | earliest registered step — usually #9 FASTSETT_MELDEPERIODER (registered for `Vurderingsbehov.alle()`); see references/flyter.md |
| saksbehandler solves a behov belonging to an earlier step | `AvklaringsbehovOrkestrator` → `forberedLøsingAvBehov` | that behov's `løsesISteg` |
| atomic revurdering inserted before this open one | `tilbakeførEtterAtomærBehandling` | earliest step whose informasjonskrav `flettOpplysningerFraAtomærBehandling` returned ENDRET |
| drift "kjør fra steg" | `Driftfunksjoner.kjørFraSteg` | sets active step to START directly (no TILBAKEFØRT rows) |

## 6. Kontekst: VurderingType and vurderingsbehov

`FlytKontekstMedPerioder` per step:
- `vurderingType` — ONE value per behandling, the highest-priority type mapped from ALL its vurderingsbehov (`prioritertType`). TypeBehandling.Førstegangsbehandling always → FØRSTEGANGSBEHANDLING unless MIGERING_FRA_ARENA. Mapping/priority: `flyt.py --vurderingtype`. Steps branch on it with an exhaustive `when` (no `else`) — keep that style so new types force a decision.
- `vurderingsbehovRelevanteForSteg` — the behandling's vurderingsbehov ∩ the list given to `medSteg(...)` for this step. **For steps after `sluttÅOppdatereFaktagrunnlag()` it is ALL vurderingsbehov** regardless of the declared list.
- `rettighetsperiode` — read from `Sak` at that moment (it can change during a behandling).
- `erFørstegangsbehandlingEllerRevurdering()` is true for FØRSTEGANGSBEHANDLING, REVURDERING **and** MIGERING_FRA_ARENA.

`vurderingsbehovRelevanteForSteg` in the flow builder does two things only: filters the kontekst and drives rewind (§5). **It never decides whether a step runs** — every step in the flow executes every time the flow passes it; the step itself must decide to do nothing (typically via `vurderingType`, `TidligereVurderinger`, `vurderingsbehovRelevanteForSteg`).

Vurderingsbehov are rows in `vurderingsbehov` (one per add, with timestamp). An event on an open ytelsesbehandling appends rows to it (no new behandling) unless the fasttrack rules in §8 apply.

## 7. Informasjonskrav

Contract (`faktagrunnlag/Informasjonskrav.kt`), run by `InformasjonskravGrunnlagImpl.oppdaterFaktagrunnlagForKravliste`:
1. `erRelevant(kontekst, steg, sisteOppdatering)` — decides if it runs now. Last-run info (`informasjonskrav_oppdatert`, per **sak** + navn) enables throttling such as `ikkeKjørtSisteKalenderdag()`; many krav are skipped for non-ytelse VurderingTypes or when `TidligereVurderinger` already gives avslag.
2. `klargjør` — sequential; DB allowed, **no external calls**.
3. `hentData` — **parallel on virtual threads; NO database access** (DBConnection is not thread-safe); external calls go here.
4. `oppdater` — sequential; DB, no external calls; returns ENDRET / IKKE_ENDRET by comparing with stored grunnlag.
`SøknadInformasjonskrav` always runs first and alone (others depend on it).

When they run: krav of steps before the active step in `forbered` (ENDRET → rewind); the active step's own krav in its OPPDATER_FAKTAGRUNNLAG phase (no rewind); steps not yet reached when the flow gets there. `flettOpplysningerFraAtomærBehandling` (default IKKE_ENDRET) merges data from a just-finished atomic revurdering into the open one. Registering a krav: `medSteg(steg = X, informasjonskrav = listOf(MyKrav), ...)` where `MyKrav` is the `companion object : Informasjonskravkonstruktør` (navn must be a new `InformasjonskravNavn`, which is persisted).

## 8. Atomic (fasttrack) behandlinger

`BehandlingService.finnEllerOpprettBehandling`: if ALL incoming vurderingsbehov are fasttrack candidates (MOTTATT_MELDEKORT, FRITAK_MELDEPLIKT, FASTSATT_PERIODE_PASSERT, UTVID_VEDTAKSLENGDE, MIGRER_RETTIGHETSPERIODE, EFFEKTUER_AKTIVITETSPLIKT(_11_9), BARNETILLEGG_SATS_REGULERING, G_REGULERING, OVERGANG_UFORE_AUTOMATISK_STANS) and
- the last ytelsesbehandling is IVERKSETTES/AVSLUTTET → new Revurdering → `MåBehandlesAtomært(ny, null)`;
- the last is an open **Revurdering** → new Revurdering inserted *before* it (`forrigeBehandlingId` relinked, grunnlag copied from the last vedtatte) → `MåBehandlesAtomært(ny, åpen)`;
- the last is an open **Førstegangsbehandling** → vurderingsbehov just added to it (ordinary).

`kjørAtomærBehandling` runs the new behandling synchronously with `stoppNårStatus = {IVERKSETTES, AVSLUTTET}` and **no savepoints**, inside the caller's transaction, then `check(status.erAvsluttet())`. If any step stops on an avklaringsbehov the check throws and the whole caller transaction (e.g. the `hendelse.håndterer` job) rolls back and is retried — fasttrack paths must be fully automatic up to FATTE_VEDTAK. Afterwards: a normal LAV-priority job finishes IVERKSETT_VEDTAK/BREV; the open behandling (if any) is rewound via `tilbakeførEtterAtomærBehandling` and re-queued; for meldekort-like behov an `flyt.informasjonskrav` job is scheduled at 19:00.

## 9. Failure semantics and savepoints (non-obvious!)

In `flyt.prosesserBehandling` jobs, `StegOrkestrator` calls `behandlingRepository.markerSavepoint()` after START and after OPPDATER_FAKTAGRUNNLAG of every step. On an exception, dbconnect's `DBTransaction` rolls back **to the last savepoint**, then turns autocommit on — which **commits everything before that savepoint**. So completed steps, their side effects (avklaringsbehov, enqueued jobs), the new step position and freshly fetched register data survive; the motor job is marked failed and retried (3 attempts back-to-back by default), resuming at the failing step from START. Atomic runs and `Driftfunksjoner`'s `flytOrkestratorUtenSavepoints` use `markSavepointAt = emptySet()` → all-or-nothing. See the motor-jobber skill for retries and blocking.

## 10. Invariants and traps (verified in code)

- `utfør` must be **idempotent** and reconcile state on every run (rerun on resume, rewind and retry). Use `AvklaringsbehovService` instead of creating/closing behov by hand.
- External side effects in steps should be enqueued as motor jobs (outbox), e.g. `IverksettVedtakSteg` → `flyt.iverksettUtbetaling` (`forSak` so utbetalinger run in order per sak).
- `behandling.flyt()` returns a **new** `BehandlingFlyt` whose cursor is the FIRST step. Call `forberedFlyt(behandling.aktivtSteg())` before `neste()`, `aktivtSteg()`, `skalOppdatereFaktagrunnlag()`, `alleFaktagrunnlagFørGjeldendeSteg()`, `gjenståendeStegIAktivGruppe()` or `tilbakeflyt*`. (Example of the bug: `BehandlingService.validerStegStatus` calls `skalOppdatereFaktagrunnlag()` on a fresh flow, so its check never fires.)
- `Status.erAvsluttet()` (behandling) is true for IVERKSETTES as well as AVSLUTTET. New events on an IVERKSETTES behandling create a NEW revurdering on top of it.
- `TidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())` looks only at checks for steps **strictly before** the given step, and only for Førstegangsbehandling/Revurdering. `TidligereVurderingerImpl` `require`s its checks to be in flow order — reordering steps can crash it at construction.
- `stegComparator`/`erStegFør` use positions in the flow; a StegType not in the flow sorts last (comparator) or gives index −1 (`erStegFør`).
- `Avklaringsbehovene` re-reads the DB on every `alle()` call; objects you hold are snapshots.

## 11. Changing a flow safely

- `StegType` names are persisted (`steg_historikk.steg`, `avklaringsbehov.funnet_i_steg`) and published in `kontrakt`: never rename/remove; old ones stay (OPPRETT_REVURDERING, VIS_GRUNNLAG; AVKLAR_STUDENT is still used by `VurderStudentSteg` while AVKLAR_STUDENT_V2 is the new one).
- Removing a step that open behandlinger sit on → "Steg X finnes ikke i flyten" when they are processed.
- Inserting a step: new behandlinger get it; open behandlinger already past it only run it after a rewind to before it.
- Moving a step earlier/later can leave open behov "behind" the active step → ERROR log + rewind, or `validerPlassering` exception.
- After editing a flow: `python3 .agents/skills/flyt-orkestrering/scripts/flyt.py --check .agents/skills/flyt-orkestrering/references/flyter.md` (regenerate with `--markdown >` if stale), update TidligereVurderingerImpl if needed, and run the closest flyt-tests in small batches (testing skill; `kjor_test.py --batch flyt` if the change is broad). Recipe for new steps: legg-til-steg skill.
