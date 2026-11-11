---
name: avklaringsbehov
description: Avklaringsbehov (human decision points and wait points) end to end — Definisjon in kontrakt (BehovType, løsesISteg, kreverToTrinn, kvalitetssikres, løsesAv), status lifecycle, måLøses/skalStoppeHer, the AvklaringsbehovService reconciliation (create/close/reopen/avbryt + tilbakestillGrunnlag), periodiserte behov, frivillige/overstyring/ventepunkt, kvalitetssikring and beslutter (totrinn), solving via the API (løsning → løser) and its validations. Use when a behov appears, disappears or reopens unexpectedly, when adding/changing a Definisjon, løsning or løser, or when debugging retur/kvalitetssikring/beslutter. Keywords — avklaringsbehov, definisjon, løsning, løser, totrinn, beslutter, kvalitetssikring, ventebehov, frivillig, overstyring.
---

# Avklaringsbehov

Code: `behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/behandling/avklaringsbehov/`
(`Avklaringsbehov.kt`, `Avklaringsbehovene.kt`, `AvklaringsbehovService.kt`, `AvklaringsbehovOrkestrator.kt`,
`ValiderBehandlingTilstand.kt`, `løsning/`, `løser/`) and `kontrakt/.../avklaringsbehov/{Definisjon,Status,AvklaringsbehovKode}.kt`.

Tools: `python3 .agents/skills/avklaringsbehov/scripts/avklaringsbehov.py [--kode 5003 | --definisjon X | --steg STEG]`
(kode, type, step, flags, roles, Løsning → Løser, which steps reference it) · generated table
[references/definisjoner.md](references/definisjoner.md).

## 1. Model

- **`Definisjon`** (kontrakt enum, published to other apps): `kode` (4 digits, `AvklaringsbehovKode`), `type: BehovType`,
  `løsesISteg: StegType` (default UDEFINERT), `kreverToTrinn`, `kvalitetssikres`, `løsesAv: List<Rolle>`, `defaultFrist`
  (ventepunkt), `vurdererVilkår`. `init` validates codes are unique and type rules.
- **BehovType**: MANUELT_PÅKREVD (only the system raises it); MANUELT_FRIVILLIG (system or saksbehandler may raise it);
  OVERSTYR (saksbehandler overrides an automatic vurdering; created on submit); VENTEPUNKT (wait with frist); BREV and
  BREV_VENTEPUNKT are deprecated. Ventepunkter MANUELT_SATT_PÅ_VENT, BESTILL_LEGEERKLÆRING, OPPRETT_HENDELSE_PÅ_SAK must
  have `løsesISteg = UDEFINERT` (= solved in the step where found); other ventepunkter need a step and a `defaultFrist`.
- **`Avklaringsbehov`**: one row per (behandling, definisjon) (unique index) + append-only history (`avklaringsbehov_endring`:
  status, begrunnelse, endretAv, frist, grunn, perioder, gradBehov, årsak til retur). `status()` = newest entry.
  `aktivHistorikk` = entries after the last AVBRUTT. `funnetISteg` = step that created it.
- **`Avklaringsbehovene`**: the behandling's behov; **re-reads the DB on every access** (`alle()`).

## 2. Status lifecycle (kontrakt `Status`)

| Status | Meaning | erÅpent | erAvsluttet |
|---|---|---|---|
| OPPRETTET | needs/accepts a løsning | ✓ | |
| AVSLUTTET | løsning received (candidate for KS/totrinn) | | ✓ |
| KVALITETSSIKRET / TOTRINNS_VURDERT | approved by kvalitetssikrer / beslutter | | ✓ |
| SENDT_TILBAKE_FRA_KVALITETSSIKRER / _FRA_BESLUTTER | rejected, must be redone | ✓ | |
| AVBRUTT | no longer relevant (soft delete) | | ✓ ← careful: "avsluttet" ≠ "løst" |

- `måLøses()` = åpent and (not MANUELT_FRIVILLIG, or `gradBehov` null/PÅKREVD, or sent back).
- `skalStoppeHer(steg)` = (`løsesISteg == steg`, or `funnetISteg == steg` when UDEFINERT) and `måLøses()` — evaluated in the
  AVKLARINGSPUNKT phase; this is what stops the flow.
- `harLøsning()` = AVSLUTTET in `aktivHistorikk`. `erTotrinn()` = `definisjon.kreverToTrinn` or a løser returned
  `LøsningsResultat(kreverToTrinn = true)`.
- Use the method `løsesISteg()` (resolves UDEFINERT → funnetISteg), not the field `definisjon.løsesISteg`, when ordering.

## 3. The reconciliation every step should use: `AvklaringsbehovService`

Steps never create/close behov by hand. They call, on every run:
- `oppdaterAvklaringsbehov(definisjon, vedtakBehøverVurdering, erTilstrekkeligVurdert, tilbakestillGrunnlag, kontekst)` (marked deprecated but widely used; maps true/false to PÅKREVD/INGEN_BEHOV), or `oppdaterAvklaringsbehovMedGrad(... behov: () -> Behov ...)`;
- `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(...)` (§4) for periodised vilkår.

Decision table (`oppdaterAvklaringsbehov`, private overload):

| Vedtak needs a human vurdering? | Existing behov | Has løsning and not `måLøsesPåNytt` | Result |
|---|---|---|---|
| yes | none | – | create OPPRETTET (with perioder, gradBehov) |
| yes | OPPRETTET | yes | AVSLUTTET ("tilstrekkelig vurdert") |
| yes | SENDT_TILBAKE_* | yes | stays open (a human rejected it); perioder updated |
| yes | AVSLUTTET / KVALITETSSIKRET / TOTRINNS_VURDERT | yes | unchanged |
| yes | OPPRETTET / SENDT_TILBAKE_* | no | stays open; perioder updated |
| yes | AVSLUTTET / KS / TOTRINNS / AVBRUTT | no | **reopened** (OPPRETTET) |
| no | none / AVBRUTT | – | nothing |
| no | anything else | – | søknad trukket or revurdering avbrutt → AVBRUTT; legacy frivillig → untouched; else **AVBRUTT + `tilbakestillGrunnlag()`** |

`måLøsesPåNytt` = a relevant vurderingsbehov is newer than the behov's last closing entry, or `!erTilstrekkeligVurdert()`
(or `perioderSomIkkeErTilstrekkeligVurdert` non-empty).

Contract for the lambdas (from the KDoc — get this right):
- *vedtakBehøverVurdering / behov* answers "will the **vedtak** contain a human vurdering of this?", not "does the current
  state need a human now". It must stay true after the saksbehandler solved it; otherwise the next run avbryter the behov
  and resets the grunnlag, i.e. the saksbehandler's work disappears.
- *erTilstrekkeligVurdert* — is the stored løsning complete/consistent enough to continue.
- *tilbakestillGrunnlag* — reset only this behov's manual vurderinger in this behandling to the forrige behandling's
  (`kontekst.forrigeBehandlingId`) values. Never touch register data or other steps' data. Called when the behov is avbrutt
  because it is no longer relevant (e.g. after a rewind changed earlier answers).

## 4. Periodised behov (`oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår`)

Inputs: `tvingerAvklaringsbehov` (vurderingsbehov that force a behov), `nårVurderingErRelevant(kontekst): Tidslinje<Boolean>`
(also exposed via `AvklaringsbehovMetadataUtleder` for the UI's "ikke relevante perioder"), `nårVurderingErGyldig`,
`gjeldendeVurderinger` (`Tidslinje<PeriodisertVurdering>` with `vurdertIBehandling`), `tilbakestillGrunnlag`.
Behov is PÅKREVD when (only for vurderingType FØRSTEGANGSBEHANDLING/REVURDERING/MIGERING_FRA_ARENA):
1. some period is relevant, and
2. a relevant vurderingsbehov is in `tvingerAvklaringsbehov`, or (frivillig) a vurdering was made in this behandling, or
   there are periods relevant now that were not relevant/sufficiently vurdert in the forrige behandling (or whose krav changed,
   toggle `NyttKravPeriodiserteAvklaringsbehov`), minus periods vurdert automatically in this behandling.
`perioderVedtaketBehøverVurdering` and `perioderSomIkkeErTilstrekkeligVurdert` are stored on the endring and drive the UI.
Reference implementation: `VurderSykdomSteg`.

## 5. Solving (API → AvklaringsbehovOrkestrator)

Full trace: flyt-orkestrering `references/sekvenser.md` §B. Validations that reject a løsning:
- behandling AVSLUTTET; `behandlingVersjon` stale (`OutdatedBehandlingException`); a `flyt.prosesserBehandling` job pending or
  FEILET (`BehandlingUnderProsesseringException`);
- behov not on the behandling (frivillig/overstyring exempt); its step is after the active step (ventebehov exempt); it belongs
  to an earlier step while the active step is after `sluttÅOppdatereFaktagrunnlag()` (exempt: 9001, 9002, 9003, 5050, 5051, 5041);
- periodised: overlapping/invalid periods; løser-specific `UgyldigForespørselException`s.
Then: rewind to the behov's step if needed → løser persists → behov AVSLUTTET → job enqueued → HTTP 202.

## 6. Frivillige, overstyring, ventepunkt

- **Legacy automatic frivillige** (`Definisjon.legacyAutomatiskFrivillgeAvklaringsbehov`): created on submit
  (`leggTilFrivilligHvisMangler`); offered to the UI by `Avklaringsbehovene.allePlussFrivillige` as **fake objects with
  id `Long.MAX_VALUE`**; never avbrutt by the service when no longer needed. AVKLAR_SAMORDNING_SYKESTIPEND joins this list only
  when toggle `DynamiskFrivilligSykestipend` is off.
- **Dynamic frivillige**: the step decides; PÅKREVD iff `gjeldendeVurderinger` contain a vurdering from this behandling.
  `ikkeEkteFrivillig` (in AvklaringsbehovService) lists definitions that are sometimes frivillig, sometimes påkrevd.
- **Overstyring** (e.g. MANUELL_OVERSTYRING_LOVVALG, OVERSTYR_IKKE_OPPFYLT_MELDEPLIKT, AVKLAR_BARNETILLEGG,
  FASTSETT_MANUELL_INNTEKT): created on submit (`leggTilOverstyringHvisMangler`).
- **Ventepunkt**: created by `FantVentebehov` from a step or by `sett-på-vent`; resolved when frist ≤ today
  (`FristUtløptVentebehovEvaluerer`) or by a `SpesifikkVentebehovEvaluerer` (sealed; new subclasses with a
  `(RepositoryProvider)` constructor are picked up by reflection), or solved manually anywhere in the flow.

## 7. Kvalitetssikring and beslutter

Revurdering/Førstegangsbehandling order: Nav-kontor steps (SAKSBEHANDLER_OPPFOLGING) → #24 KVALITETSSIKRING (KVALITETSSIKRER)
→ NAY steps (SAKSBEHANDLER_NASJONAL) → #53 FORESLÅ_VEDTAK (NAY) → #54 FATTE_VEDTAK (BESLUTTER) → #55 IVERKSETT_VEDTAK →
#56 BREV (SKRIV_VEDTAKSBREV, BESLUTTER).
- KVALITETSSIKRING is needed iff the *faktisk* behandlingstype (`BehandlingService.utledFaktiskBehandlingstype`: a revurdering
  where the forrige had no rett counts as førstegangsbehandling) is Førstegangsbehandling or Klage and a non-avbrutt behov has
  `kvalitetssikres`. Re-required when any such vurdering changed after the last kvalitetssikring (`VurderingEndretService`).
- FORESLÅ_VEDTAK is needed iff there is behandlingsgrunnlag and NAY has solved something (special case: only lovvalg after sykdom → no).
- FATTE_VEDTAK is needed iff a non-avbrutt behov `erTotrinn()` (not when klage opprettholdes, trukket, no grunnlag, or toggle
  `HoppOverBeslutterVedAvslagSykdom` applies). `FatteVedtakSteg` stores the vedtak (vedtakstidspunkt = last AVSLUTTET of FATTE_VEDTAK).
- Return: the løser sets SENDT_TILBAKE_* on the rejected behov only; the next run rewinds to the earliest of them.
- Four-eyes rule: `KanIkkeVurdereEgneVurderingerException` if the approver solved one of the behov — **skipped when toggle
  `IngenValidering` is on, which it is in `LokalUnleash` and `AlleAvskruddUnleash`**, so tests don't catch violations.

## 8. Adding or changing a Definisjon (summary — full recipe in legg-til-steg)

1. kontrakt: new `const val X_KODE` + enum value in `AvklaringsbehovKode.kt`, new `Definisjon` entry. Codes and names are
   persisted and used by other apps: never reuse or rename; deprecate instead. Bump the kontrakt version in aap-tilgang,
   aap-oppgave and aap-statistikk (see `kontrakt/.../README.md`).
2. `løsning/XLøsning.kt` with `@JsonTypeName(value = X_KODE)` implementing `EnkeltAvklaringsbehovLøsning` or
   `PeriodisertAvklaringsbehovLøsning<…>` (sealed: must live in that package) → `løs()` delegates to `løser/XLøser.kt`
   (`AvklaringsbehovsLøser<XLøsning>`, also sealed).
3. In the step whose `StegType == løsesISteg`: call `AvklaringsbehovService` every run.
4. Grunnlag endpoint in `api` (use `Definisjon.X.løsesAv` for `påkrevdRolle`), register in `App.kt`.
5. Regenerate: `avklaringsbehov.py --markdown > .agents/skills/avklaringsbehov/references/definisjoner.md`.

## 9. Traps

- `Avklaringsbehovene.leggTil` is deprecated for steps: it silently ignores an already open behov and reopens closed ones.
- Avklaringsbehov are **not copied** to a revurdering; previous answers live in grunnlag reached via `forrigeBehandlingId`.
- `Status.erAvsluttet()` includes AVBRUTT; use `harLøsning()` / `status() == AVSLUTTET` for "solved".
- A step may only create behov for itself or later steps (earlier → `IllegalStateException` in `validerPlassering`).
- Changing `løsesISteg` of an existing Definisjon moves open behov between steps — treat like a step-order change.
