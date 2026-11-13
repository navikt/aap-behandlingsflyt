---
applyTo: "behandlingsflyt/src/main/kotlin/**/forretningsflyt/**,**/*Steg.kt"
description: "Steg og flytdefinisjoner: idempotent utfør, vurderingType, avklaringsbehov via service, stegrekkefølge"
---
# Regler for steg og flytdefinisjoner

- `utfør(kontekst)` kjøres mange ganger for samme behandling. Skriv den som en avstemming: les det som finnes,
  regn ut ønsket tilstand, skriv bare hvis noe er endret. Aldri "insert uten å sjekke".
- Styr oppførsel på `kontekst.vurderingType` og `kontekst.vurderingsbehovRelevanteForSteg`, aldri på
  `behandling.typeBehandling()`. Husstilen er uttømmende `when` over alle `VurderingType`-verdier, ikke `else`.
- Avklaringsbehov opprettes/lukkes bare via `AvklaringsbehovService.oppdaterAvklaringsbehov*`. Send inn ekte
  `erTilstrekkeligVurdert` og `tilbakestillGrunnlag`; `{}` som tilbakestilling gir gamle vilkårsresultat i vedtaket.
- Et steg kan ikke "returnere" et avklaringsbehov; motoren leser repositoryet i `AVKLARINGSPUNKT`. Returverdiene er
  kun `Fullført` eller `FantVentebehov`.
- Tilbakeføring kjører ingen stegkode. Rydd derfor opp i egne vurderinger/vilkårsresultat når steget kjører igjen
  og grunnlaget ikke lenger er relevant.
- Ikke endre rekkefølgen i `Revurdering.kt`/`Klage.kt` m.fl. uten at mennesket har bekreftet: indekser brukes til
  tilbakeføring, og flytsekvenstestene (`*FlytOrkestratorTest`, `KlageFlytTest`) asserterer eksakt rekkefølge.
- Nytt steg: `companion object : FlytSteg` med `konstruer` og `type()`, `StegType` i kontrakt (kontraktendring!),
  `.medSteg(...)` i riktig(e) flyt(er), evt. visning i `flyt/flate/visning/`, `*GrunnlagApi` i `api` + rute i `App.kt`.
- Test: `XxxStegTest` som kjører `utfør` to ganger på samme kontekst og asserterer identisk grunnlag og ingen
  dupliserte avklaringsbehov. Oppdater flytsekvenstestene. Mal: `KravStegTest`, `VurderAktivitetsplikt11_7StegTest`.
