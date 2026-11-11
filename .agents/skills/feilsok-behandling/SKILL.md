---
name: feilsok-behandling
description: Diagnose a behandling or job that is stuck, failing or behaving unexpectedly in dev/prod or locally — symptom → cause table, exact error messages and what they mean, read-only SQL to reconstruct what happened (steg_historikk, avklaringsbehov, vurderingsbehov, jobb), drift API and motor-api endpoints for reprocessing, and safe manual fixes. Use for "behandlingen henger", "prosesseringen feilet", "kan ikke løse avklaringsbehov", unexpected tilbakeføring or reopened behov, or job failures. Keywords — feilsøking, drift, henger, feilet jobb, BehandlingUnderProsesseringException, OutdatedBehandlingException, rekjør, kjør fra steg, playbook.
---

# Troubleshooting behandlinger and jobs

Read-only SQL: [references/sql.md](references/sql.md). Past incidents and a mass-reprocessing example: `docs/playbook.md`.
Mechanics: flyt-orkestrering (engine, rewinds), motor-jobber (jobs, retries), avklaringsbehov (behov lifecycle).
Production data is personal data: prefer IDs (behandling referanse, saksnummer) over idents; don't paste personal data.

## 1. First look (in this order)

1. Identify: behandling `referanse` (UUID) → `behandling.id`, `sak_id`, `type`, `status`, `versjon` (sql.md §1).
2. Jobs for the behandling/sak: any KLAR/FEILET `flyt.prosesserBehandling`? FEILET → read `jobb_historikk.feilmelding` (§4).
   Also `hendelse.håndterer` for the sak (incoming documents are processed per sak, in order — a FEILET one blocks all later documents).
3. Where is it: latest `steg_historikk` rows (§2) — active step + phase. AVKLARINGSPUNKT = waiting for a behov;
   other phases = it stopped mid-step (failure or still running).
4. Why does it stop there: open avklaringsbehov (§3). Ventebehov with frist? Behov with `løsesISteg` before the active step?
5. Why did it move back: vurderingsbehov timestamps (§5), TILBAKEFØRT rows (§2), informasjonskrav changes (§6), retur
   (SENDT_TILBAKE_* in §3).

## 2. Symptom → likely cause

| Symptom | Likely cause / check |
|---|---|
| UI "prosesserer…" forever / FEILET | KLAR or FEILET `flyt.prosesserBehandling` job (jobs §4). FEILET = 3 quick attempts failed; retried once at 07/12/15/20 |
| Behandling stuck, no open behov, no job | nothing triggered it (e.g. trigger was a no-op because a job existed, then that job failed and was avbrutt). Reprocess via drift (§4) |
| Waits although frist passed | `batch.gjenopptaBehandlinger` runs 07:00 daily; or its job failed; or another ventebehov is still open |
| Document received but nothing happened | `hendelse.håndterer` job FEILET/KLAR for the sak; dedupe (same referanse already in `mottatt_dokument`); NY_ÅRSAK on an avsluttet behandling is ignored (WARN) |
| Saksbehandler's vurdering "disappeared" | the step decided the vedtak no longer needs it → behov AVBRUTT + `tilbakestillGrunnlag` (avklaringsbehov skill §3); often after a rewind changed earlier answers |
| Behov reopened without anyone touching it | new vurderingsbehov newer than the behov's last AVSLUTTET (document, manual revurdering årsak), or `erTilstrekkeligVurdert` became false |
| Behandling jumped back many steps | new/renewed vurderingsbehov → rewind to earliest registered step (often #9 FASTSETT_MELDEPERIODER); informasjonskrav ENDRET; retur from beslutter/kvalitetssikrer |
| A second revurdering appeared | the open one was IVERKSETTES (counts as avsluttet) or a fasttrack (meldekort etc.) atomic revurdering was inserted before the open one |
| Meldekort/hendelse job keeps failing with "må kjøres atomært" | the atomic revurdering stopped on an avklaringsbehov; find which step/behov in the logs of that job |

## 3. Error messages

| Message (grep for it) | Meaning |
|---|---|
| `Behandlingen har prosesseringsjobber som venter eller har feilet` (BehandlingUnderProsesseringException) | KLAR/FEILET `flyt.prosesserBehandling` exists; løs-behov refused |
| `Behandlingen har blitt oppdatert i bakgrunnen` (OutdatedBehandlingException) | `behandlingVersjon` from the UI ≠ `behandling.versjon`; reload |
| `Behandlingen er avsluttet og kan ikke lenger endres` | AVSLUTTET (also thrown if a processing job runs on an AVSLUTTET behandling) |
| `Forsøker å løse avklaringsbehov '…' som ikke hører til behandlingen` | behov does not exist (step not reached yet, or never created) |
| `Aktivt steg X må løses før du kan løse Y` | Y's step is after the active step |
| `Forsøker å løse avklaringsbehov … definert i et steg før nåværende steg` | active step is after `sluttÅOppdatereFaktagrunnlag()` (FATTE_VEDTAK and later) |
| `Har uhåndterte behov som skulle vært håndtert før nåværende steg` | a step created a behov for an EARLIER step, or step order changed (IllegalStateException) |
| `…har passert det åpne avklaringsbehovet … Med mindre det har skjedd en endring i rekkefølgen av stegene, så er dette en bug` | ERROR log in forbered, followed by a rewind; investigate how the behov got behind |
| `Aktivt steg X er ikke lik det forventede steget Y` | solving: rewind to the behov's step didn't land there (e.g. IVERKSETTES behandling can't rewind) |
| `Steg X finnes ikke i flyten` | persisted StegType no longer in the flow (step removed/renamed) |
| `Behandlingen er avsluttet, men det finnes avklaringsbehov som må løses` | end of flow reached with a måLøses behov open |
| `må kjøres atomært, men er ikke i avsluttet tilstand` | fasttrack behandling stopped on a behov |
| `Prøvde å tilbakeføre avsluttet eller iverksatt behandling` (WARN) | rewind ignored for IVERKSETTES/AVSLUTTET |
| `Kan ikke vurdere kvalitet/to-trinn på egne vurderinger.` | four-eyes rule (KanIkkeVurdereEgneVurderingerException) |
| `Feil under prosessering av jobb` / `Feil under plukking av jobber` | motor; stack trace in secure logs and `jobb_historikk.feilmelding` |
| `Kun én jobb skal bli markert …` | motor bookkeeping race; job ran but status update failed (at-least-once) |

## 4. Tools to act (need DRIFTE / team roles in prod; use with care)

behandlingsflyt drift API (`api/.../drift/DriftApi.kt`, all under `/api/drift`):
- `POST /behandling/{referanse}/prosesser` body `{"skalForberede": true}` — runs forbered+prosesser synchronously in the request
  (with savepoints). `false` skips forbered (no rewinds/informasjonskrav).
- `POST /behandling/{referanse}/kjor-fra-steg` body `{"steg": "AVKLAR_SYKDOM"}` — only UTREDES behandlinger, only to an earlier
  UTREDES step in its flow; sets the active step to START directly (no TILBAKEFØRT rows) and prosesserer.
- Read-only helpers: `/sak/{saksnummer}/info`, `/sak/{saksnummer}/mottatte-dokumenter`, `/behandling/{referanse}/vilkår`,
  `/tidligere-vurderinger`, `/tilkjent-ytelse`, `/rettighetsinfo`, `/person`; `/brev/{ref}/avbryt` cancels a vedtaksbrev bestilling.
motor-api (`/drift/api/jobb`): `GET /feilende`, `/planlagte-jobber`, `/sisteKjørte`, `GET /rekjor/{jobbId}`,
`/rekjorAlleFeilede`, `/avbryt/{jobbId}`, `/avbrytAlleFeilede`.
Local: `curl -s -XPOST http://localhost:8081/token | jq -r .access_token` gives a token for TestApp (README).

Manual SQL changes are a last resort: when re-queueing jobs, insert with `kjorbar = false` (sql.md §8) and spread load
(the 2026-10 incident in `docs/playbook.md` created ~1000 jobs at once with `kjorbar = true` and slowed Kelvin).
Fixing data by editing child rows is dangerous because rows are shared between behandlinger (repository-og-migrering §2).
