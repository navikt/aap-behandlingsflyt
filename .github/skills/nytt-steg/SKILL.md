---
name: nytt-steg
description: Oppskrift for å legge til et nytt steg i behandlingsflyten (StegType, Steg-klasse, flytdefinisjon, visning, grunnlag-API, tester). Bruk når noen ber om et nytt steg eller å "legge noe inn i flyten".
---
# Nytt steg i flyten

Før du starter: les `.github/instructions/steg.instructions.md`. Et nytt steg endrer publisert kontrakt (`StegType`)
og flytrekkefølgen, som brekker åpne behandlinger hvis det gjøres feil. Avklar med mennesket:
1. Hvilken flyt (Revurdering/Klage/…) og **hvor** i rekkefølgen? Hvilke `Vurderingsbehov` gjør steget relevant?
2. Skal steget vises i frontend (steggruppe)? Trenger det eget grunnlag-API?
3. Finnes det et lignende steg å kopiere? (Finn det med `grep -l "companion object : FlytSteg"` og velg ett som
   bruker `AvklaringsbehovService.oppdaterAvklaringsbehov` riktig, f.eks. `VurderAvslag11_27Steg`.)

Reelt eksempel: commit `6d0768796`, "Legger til støtte for vurdering av avslag 11-27", med `VurderAvslag11_27Steg`.
Commiten rører 44 filer fordi den også legger til avklaringsbehov, løser og grunnlag med repository; de delene dekkes av
skillene `nytt-avklaringsbehov` og `nytt-grunnlag`. Selve steget er `StegType`, `StegGruppe`, `VurderAvslag11_27Steg`,
`Revurdering.kt`, `Vurderingsbehov`, `Avslag11_27VisningUtleder`, `Avslag11_27GrunnlagApi` + `App.kt` og
`EnklereFlytOrkestratorTest`.

1. `kontrakt/.../steg/StegType.kt`: ny enum-verdi med `gruppe` og `status`. Dette er en kontraktendring – si fra.
2. `behandlingsflyt/.../forretningsflyt/steg/XxxSteg.kt` (eller i feature-pakken `no.nav.aap.<feature>`):
   `class XxxSteg(avhengigheter) : BehandlingSteg` + `companion object : FlytSteg { konstruer(repositoryProvider,
   gatewayProvider); type() }`. Har steget et avklaringsbehov: kall `avklaringsbehovService.oppdaterAvklaringsbehov(
   kontekst, definisjon, vedtakBehøverVurdering, erTilstrekkeligVurdert, tilbakestillGrunnlag)` og returner `Fullført`.
   `vedtakBehøverVurdering` har en uttømmende `when (kontekst.vurderingType)` uten `else`.
3. Flytdefinisjon: `.medSteg(steg = XxxSteg, vurderingsbehovRelevanteForSteg = …, informasjonskrav = …)` på riktig
   plass i `Revurdering.kt` (og andre flyter hvis relevant).
4. Visning: `flyt/flate/visning/*VisningUtleder.kt` / `DynamiskStegGruppeVisningService` hvis steget skal vises.
5. API: `api/.../<feature>/XxxGrunnlagApi.kt` + `XxxGrunnlagResponse.kt`, monter i `app/.../App.kt`. Bruk
   `getGrunnlag<BehandlingReferanse, XxxGrunnlagDto>(..., påkrevdRolle = Definisjon.XXX.løsesAv)`, ikke `authorizedGet`:
   eksempelcommiten brukte `authorizedGet` og ble rettet i `da1aaee0b` fordi `kanSaksbehandle()` ellers ikke hentes riktig.
6. Tester: `XxxStegTest` (kjør `utfør` to ganger; ingen duplikater, og tilbakestilling henter fra `forrigeBehandlingId`).
   Eksempelcommiten mangler denne testen; skriv den likevel. Oppdater `EnklereFlytOrkestratorTest`/`KlageFlytTest` eller
   tilsvarende sekvenstester, test visningsutlederen, og én scenariotest hvis steget stopper flyten.
7. Kjør skillen `verifiser`. Nevn i PR-beskrivelsen at kontrakt og flytrekkefølge er endret, og hvilke konsumenter
   som må bumpe.
