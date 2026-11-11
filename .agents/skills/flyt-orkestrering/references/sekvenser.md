# Scenario traces

Concrete, code-verified traces of what happens, in order. Class/method names are the ones to grep for.
"Job X" = motor job of type X (see motor-jobber skill). Step numbers refer to the Revurdering/Førstegangsbehandling
flow (`flyt.py --flyt Revurdering`).

## A. New søknad → first stop

1. Postmottak calls `POST /api/sak/finnEllerOpprett` (app role `opprett-sak`) → `PersonOgSakService.finnEllerOpprett` creates
   person + sak (saksnummer, rettighetsperiode from søknadsdato).
2. `POST /api/hendelse/send` (or `/api/hendelse/sak/{saksnummer}/send`) → `MottattHendelseService.registrerMottattHendelse`:
   ignore if (sak, type, referanse) already received → enqueue job `hendelse.håndterer` (`forSak`, prioritet by type).
3. Job `hendelse.håndterer` (`HendelseMottattHåndteringJobbUtfører`): lock sak → dedupe again → store `mottatt_dokument`
   → `HåndterMottattDokumentService.håndterMottatteDokumenter`:
   `MottattHendelseUtleder` gives vurderingsbehov MOTTATT_SØKNAD + årsak SØKNAD →
   `BehandlingService.finnEllerOpprettBehandling` → no ytelsesbehandling → new Førstegangsbehandling (status OPPRETTET, no
   steg_historikk) → `SakService.oppdaterRettighetsperioden` (fom = min(fom, mottatt dato), tom = `Tid.MAKS`) → link document →
   `ProsesserBehandlingService.triggProsesserBehandling` → job `flyt.prosesserBehandling`.
4. Job `flyt.prosesserBehandling`: forbered (sak → UTREDES; nothing to refresh before step 1) → prosesser from #1
   START_BEHANDLING (OPPDATER_FAKTAGRUNNLAG fetches Søknad + Barn; `StartBehandlingSteg` adds IKKE_VURDERT periods for all
   obligatoriske vilkår) → #2 KRAV … each step: START, OPPDATER_FAKTAGRUNNLAG (its own krav, e.g. #8 Personopplysning/Lovvalg),
   UTFØRER, AVKLARINGSPUNKT, AVSLUTTER.
5. First step that leaves a must-solve behov for itself (often #13 AVKLAR_SYKDOM; earlier if e.g. lovvalg needs manual
   vurdering) → Stopp at AVKLARINGSPUNKT → `BehandlingHendelseService.stoppet` enqueues `flyt.hendelse` (oppgave creates a
   task), `flyt.statistikk`, datadeling and meldekort jobs. Transaction commits.

## B. Saksbehandler solves an avklaringsbehov

1. Frontend reads `GET /api/behandling/{ref}/flyt` (includes `behandlingVersjon`) and the grunnlag endpoint for the step.
2. `POST /api/behandling/løs-behov` (or `/løs-periodisert-behov`) `{referanse, behandlingVersjon, behov: {behovstype: "5003", …}}`.
   In one transaction (`AvklaringsbehovApi`):
   - lock sak + behandling; `BehandlingTilstandValidator`: versjon must equal current (`OutdatedBehandlingException`), and no
     KLAR/FEILET `flyt.prosesserBehandling` job may exist (`BehandlingUnderProsesseringException`).
   - `AvklaringsbehovOrkestrator.løsAvklaringsbehovOgFortsettProsessering`:
     a. `validerTilstand(behandling, definisjon)`: not AVSLUTTET; behov exists (frivillig/overstyring exempt); its step is not
        after the active step (ventebehov exempt); not an earlier-step behov once the active step is after
        `sluttÅOppdatereFaktagrunnlag()` (exempt codes 9001, 9002, 9003, 5050, 5051, 5041).
     b. periodised løsning → `AvklaringsbehovValidering.validerPerioder`.
     c. `FlytOrkestrator.forberedLøsingAvBehov`: create legacy-frivillig/overstyring behov if missing; **rewind to the behov's
        step if the behandling is past it**; then require active step == that step (ventebehov exempt).
     d. `løsning.løs(...)` → the Løser validates and persists the vurdering → `LøsningsResultat(begrunnelse, kreverToTrinn?)`.
     e. `Avklaringsbehovene.løsAvklaringsbehov` → status AVSLUTTET (+ kreverToTrinn).
     f. enqueue `flyt.prosesserBehandling`; delete mellomlagret vurdering.
   - `verifiserSkrivelås` bumps `versjon` → HTTP 202.
3. Job runs: resumes the step from START → `AvklaringsbehovService` finds a sufficient løsning → behov stays AVSLUTTET →
   flow continues to the next stop. Any later step's behov may be reopened if their inputs changed.

## C. Return from kvalitetssikrer / beslutter

1. Beslutter submits `FatteVedtakLøsning` (one vurdering per totrinn-definisjon, `godkjent` + grunner).
   `FatteVedtakLøser`: if the beslutter has solved any of the totrinn behov → `KanIkkeVurdereEgneVurderingerException`
   (disabled by toggle `IngenValidering`, which is ON in test fakes). If any `godkjent=false`: only those become
   SENDT_TILBAKE_FRA_BESLUTTER; else all relevant become TOTRINNS_VURDERT. FATTE_VEDTAK itself → AVSLUTTET.
2. Job: forbered §3.5 finds the returned behov (open again) before FATTE_VEDTAK → rewind to its step → prosesser → that step's
   `AvklaringsbehovService` keeps it open (status SENDT_TILBAKE_*) → stop.
3. Saksbehandler re-solves → AVSLUTTET → flow runs on. `KvalitetssikringsSteg` reopens KVALITETSSIKRING if vurderinger
   changed since the last kvalitetssikring (only faktisk førstegangsbehandling and klage); `FatteVedtakSteg` reopens
   FATTE_VEDTAK while any totrinn behov is not TOTRINNS_VURDERT.
Kvalitetssikring (#24, role KVALITETSSIKRER) works the same with SENDT_TILBAKE_FRA_KVALITETSSIKRER and only covers behov
with `kvalitetssikres = true` (Nav-kontor steps before #24).

## D. New document / vurderingsbehov on an open behandling

1. e.g. legeerklæring → job `hendelse.håndterer` → `finnEllerOpprettBehandling`: open ytelsesbehandling exists → append
   MOTTATT_LEGEERKLÆRING rows (timestamp now) via `oppdaterVurderingsbehovOgÅrsak` → enqueue `flyt.prosesserBehandling`.
   (Behandling IVERKSETTES/AVSLUTTET instead → a new Revurdering is created.)
2. Job forbered: an open BESTILL_LEGEERKLÆRING ventebehov is resolved by `LegeerklæringVentebehovEvaluerer` (rewind to where it
   waited); then §3.7b: the new vurderingsbehov is newer than the last run of registered steps → rewind to the earliest
   registered step (#9 FASTSETT_MELDEPERIODER for most vurderingsbehov, see references/flyter.md).
3. Prosesser re-runs from there. Steps registered for MOTTATT_LEGEERKLÆRING see it in `vurderingsbehovRelevanteForSteg`;
   `AvklaringsbehovService` reopens their AVSLUTTET behov because the vurderingsbehov is newer than the behov's last
   AVSLUTTET (`vurderingsbehovetErNyereEnnAvklaringsbehovet`) when the step declares a need (PÅKREVD).

## E. Meldekort on a sak (fasttrack / atomic)

1. `hendelse.håndterer` with only MOTTATT_MELDEKORT:
   - last ytelsesbehandling IVERKSETTES/AVSLUTTET → new Revurdering (grunnlag copied) → `MåBehandlesAtomært(ny, null)`;
   - open Revurdering → new Revurdering inserted between the last vedtatte and the open one (open.`forrigeBehandlingId` := ny)
     → `MåBehandlesAtomært(ny, åpen)`;
   - open Førstegangsbehandling → just appended to it (ordinary, case D).
2. `ProsesserBehandlingService.kjørAtomærBehandling` in the SAME transaction: atomic orchestrator (stop at IVERKSETTES, no
   savepoints) runs `ny` through all steps; MeldekortInformasjonskrav (#49) picks up unprocessed meldekort;
   VurderingType MELDEKORT makes most steps no-ops; must reach IVERKSETT_VEDTAK without stopping or
   `check(status.erAvsluttet())` fails and the whole `hendelse.håndterer` transaction rolls back (retry).
3. Then: LAV-priority `flyt.prosesserBehandling` for `ny` (IVERKSETT_VEDTAK → utbetaling job, BREV) ; for `åpen`:
   `tilbakeførEtterAtomærBehandling` (rewind to earliest step whose krav' `flettOpplysningerFraAtomærBehandling` is ENDRET, e.g.
   meldekort) + enqueue its job; without `åpen`: `flyt.informasjonskrav` job scheduled ≥ 19:00.

## F. Waiting (sett på vent) and resuming

1. Manual: `POST /api/behandling/{ref}/sett-på-vent` → `AvklaringsbehovOrkestrator.settBehandlingPåVent` → behov
   MANUELT_SATT_PÅ_VENT (VENTEPUNKT, løsesISteg UDEFINERT → funnetISteg = active step, default frist 3 weeks) → stoppet-hendelse.
   No flow run. From a step: `return FantVentebehov(Ventebehov(definisjon, grunn, frist))` (definisjon must be a ventebehov,
   frist in the future) → behov created in UTFØRER → stop in AVKLARINGSPUNKT of that step.
2. While waiting, other triggers may still process the behandling; it stops at the waiting step again.
3. Resume: daily job `batch.gjenopptaBehandlinger` (07:00) enqueues processing for behandlinger with an OPPRETTET behov whose
   frist ≤ today → forbered resolves it (`FristUtløptVentebehovEvaluerer`) → rewind to the waiting step → continue.
   Or the saksbehandler solves the ventebehov (`SattPåVentLøsning`) — allowed regardless of step order.

## G. A step throws

1. Step X's UTFØRER throws (e.g. a gateway timeout in a hentData, an `IllegalStateException`).
2. `DBTransaction` rolls back to the savepoint taken after X's OPPDATER_FAKTAGRUNNLAG, then commits the rest (steps before X,
   X's START/OPPDATER_FAKTAGRUNNLAG rows, fetched register data). Motor records a FEILET history row; attempts 2 and 3 start
   immediately (no backoff); after the 3rd failure the job is FEILET.
3. While FEILET: the job's group is blocked, new triggers are no-ops, `løs-behov` → `BehandlingUnderProsesseringException`,
   `/flyt/prosessering` shows FEILET. Recovery: fix → `oppgave.retryFeilede` (07/12/15/20) or `GET /drift/api/jobb/rekjor/{id}`.
