---
applyTo: "behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/faktagrunnlag/**"
description: "Informasjonskrav og grunnlag: de fire fasene, ENDRET og tilbakeføring, rate-limiting"
---
# Regler for informasjonskrav og grunnlag

- Fire faser med streng arbeidsdeling (`Informasjonskrav.kt` KDoc): `erRelevant` (gate/rate-limit),
  `klargjør` (kun DB-lesing), `hentData` (kun eksternt kall; kjører på virtuell tråd uten DB-tilkobling),
  `oppdater` (sammenlign med lagret, skriv, returner `ENDRET`/`IKKE_ENDRET`).
- `ENDRET` tilbakefører behandlingen til det tidligste steget som har kravet i `kravliste`. Et krav som alltid
  svarer `ENDRET` gir evig tilbakeføring; et som aldri svarer `ENDRET` oppdaterer aldri. Sammenlign på innhold.
- Bruk `oppdatert.ikkeKjørtSisteKalenderdagForBehandling(behandlingId)` eller input-sammenligning som rate-limiter,
  slik de fleste krav gjør.
- Krav i samme steg må være uavhengige av hverandre (`SøknadInformasjonskrav` kjøres først og synkront nettopp fordi
  dette ble brutt én gang).
- `InformasjonskravNavn` lagres i DB: legg til, aldri omdøp/slett. Registrer kravet i riktig(e) stegs `kravliste`
  i flytdefinisjonen, og nye dokumenttyper i inntaks-`when` i `HendelseMottattHåndteringJobbUtfører`.
- Dokumentkrav må markere dokumenter som behandlet (`markerSomBehandlet`); ellers inntas de på nytt hver kjøring.
- Grunnlagsmodeller: `data class`, proveniens-felt (`vurdertIBehandling`, `opprettet`, `Bruker`). Repository-grensesnitt
  (`XxxRepository : Repository` fra `no.nav.aap.lookup.repository`) ligger her; implementasjonen i `repository/`.
- Test: `XxxInformasjonskravTest` med par `IKKE_ENDRET` på identiske data / `ENDRET` på ett endret felt, og
  `erRelevant` per VurderingType. Mal: `YrkesskadeInformasjonskravTest`.
