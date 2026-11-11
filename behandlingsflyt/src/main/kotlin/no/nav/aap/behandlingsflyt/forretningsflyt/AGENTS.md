# Flyter og steg (`forretningsflyt/`) — tillegg til AGENTS.md i rota (les den også)

Last inn skillene `flyt-orkestrering` (hvordan flytene kjøres) og `legg-til-steg` (oppskrifter) før du redigerer her.

- `behandlingstyper/Revurdering.kt` definerer flyten for både Revurdering og Førstegangsbehandling (`Førstegangsbehandling.flyt()`
  bruker den). Få oversikt uten å lese 500 linjer: `python3 .agents/skills/flyt-orkestrering/scripts/flyt.py --flyt Revurdering`.
- Endringer i flyten (fjerne, gi nytt navn til eller endre rekkefølgen på steg) kan ødelegge åpne og avsluttede behandlinger: `StegType`-navn
  lagres og publiseres i `kontrakt`. Hvis et steg fjernes mens behandlinger står på det, får de feilen «Steg X finnes ikke i flyten».
- `vurderingsbehovRelevanteForSteg` filtrerer `kontekst.vurderingsbehovRelevanteForSteg` og avgjør hvilke nye vurderingsbehov
  som fører flyten tilbake til steget. Den hopper aldri over et steg. Hvert steg kjøres ved hver gjennomgang.
- Stegets `informasjonskrav` oppdateres i fasen OPPDATER_FAKTAGRUNNLAG og, for steg før det aktive steget, i
  `forbered` (der ENDRET fører flyten tilbake). Steg etter `.sluttÅOppdatereFaktagrunnlag()` får ingen oppdatering i `forbered` og
  mottar ALLE vurderingsbehov i konteksten.
- `TidligereVurderingerImpl` må liste kontrollene i flytrekkefølge (`require` krever dette når objektet opprettes).
- Hver `utfør` må være idempotent og avstemme avklaringsbehov gjennom `AvklaringsbehovService` (avbryt + `tilbakestillGrunnlag`
  når behovet ikke lenger er relevant). Bruk motor-jobber for eksterne sideeffekter.
- Etter at du har endret en flyt: Kjør `flyt.py --check .agents/skills/flyt-orkestrering/references/flyter.md` (generer på nytt med
  `--markdown >` hvis den er utdatert), og kjør den nærmeste `*FlytTest`-testen.
