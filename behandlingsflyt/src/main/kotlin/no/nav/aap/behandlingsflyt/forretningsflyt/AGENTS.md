# Flows and steps (`forretningsflyt/`) — supplements the root AGENTS.md (read that too)

Load skills `flyt-orkestrering` (how flows execute) and `legg-til-steg` (recipes) before editing here.

- `behandlingstyper/Revurdering.kt` is the flow for BOTH Revurdering and Førstegangsbehandling (`Førstegangsbehandling.flyt()`
  delegates to it). Overview without reading 500 lines: `python3 .agents/skills/flyt-orkestrering/scripts/flyt.py --flyt Revurdering`.
- Changing the flow (removing, renaming or reordering steps) can break open and closed behandlinger: `StegType` names are
  persisted and published in `kontrakt`. Removed steps that behandlinger sit on give "Steg X finnes ikke i flyten".
- `vurderingsbehovRelevanteForSteg` filters `kontekst.vurderingsbehovRelevanteForSteg` and decides which new vurderingsbehov
  rewind the flow to the step. It never skips a step — every step executes on every pass.
- `informasjonskrav` on a step are refreshed in its OPPDATER_FAKTAGRUNNLAG phase and, for steps before the active one, in
  `forbered` (where ENDRET rewinds the flow). Steps after `.sluttÅOppdatereFaktagrunnlag()` get no refresh in `forbered` and
  receive ALL vurderingsbehov in their kontekst.
- `TidligereVurderingerImpl` must list its checks in flow order (it `require`s this when constructed).
- Every `utfør` must be idempotent and reconcile avklaringsbehov via `AvklaringsbehovService` (avbryt + `tilbakestillGrunnlag`
  when no longer relevant). External side effects go through motor jobs.
- After editing a flow: `flyt.py --check .agents/skills/flyt-orkestrering/references/flyter.md` (regenerate with
  `--markdown >` when stale) and run the closest `*FlytTest`.
