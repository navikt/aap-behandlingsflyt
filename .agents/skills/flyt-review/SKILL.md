---
name: flyt-review
description: Sjekkliste-gjennomgang av en diff som rører flyt, steg, avklaringsbehov, informasjonskrav, grunnlag, kontrakt eller migrasjoner. Bruk på egen diff før PR, eller på en PR som skal reviewes.
---
# Gjennomgang av flytendringer

Gå gjennom diffen (`git diff main...HEAD` eller PR-nummer) punkt for punkt og svar ja/nei/ikke relevant med
filreferanse. Ikke godkjenn på magefølelse; finn beviset i diffen.

**Historikk og kontrakt**
- Er noen eksisterende verdi i `Definisjon`, `StegType`, `AvklaringsbehovKode`, `Vurderingsbehov`, `InformasjonskravNavn`
  eller en jobbtype-streng endret/fjernet/omdøpt? (Skal være nei.)
- Rører PR-en `kontrakt/`? Da: hvilke konsumenter må bumpe, og står det i PR-beskrivelsen?
- Er stegrekkefølge endret? Er flytsekvenstestene oppdatert bevisst, eller bare "fikset"?

**Idempotens og gjenkjøring**
- Kan `utfør`/`løs`/`oppdater` kjøres to ganger uten duplikater? Finnes det en test som gjør det?
- Brukes `AvklaringsbehovService.oppdaterAvklaringsbehov*` med ekte `tilbakestillGrunnlag`?
- Branches koden på `typeBehandling()` der den burde bruke `vurderingType`/`vurderingsbehov`?

**Data**
- `UPDATE`/`DELETE` på grunnlags-/vurderingsrader utenom `aktiv=false` og `slett()`?
- Nytt repository: `kopier` + `slett` implementert, registrert i Postgres- og InMemory-registry, DB-test finnes?
- Migrasjon rullende-trygg (nullable/DEFAULT, ingen DROP/RENAME sammen med kodeendring)?

**Informasjonskrav**
- `hentData` uten DB, `klargjør`/`oppdater` uten eksterne kall? `ENDRET` bare ved reell endring? Rate-limit i `erRelevant`?

**Registreringer**
- Nye klasser registrert i `PostgresRepositoryRegistry`, `DefaultGatewayProvider`, `TestGatewayProvider`,
  `InMemoryRepositoryRegistry`, `ProsesseringsJobber`, `App.kt`, `FakeUnleash`?

**Tester og PR**
- Er det test på riktig nivå (steg/løser/krav-test, ikke bare 5-sekunders scenariotest)?
- Sier PR-beskrivelsen hva som er intensjonen og hvordan det er testet?

Avslutt med en kort liste over funn sortert etter alvorlighet, hver med fil:linje.
