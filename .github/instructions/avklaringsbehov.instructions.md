---
applyTo: "behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/behandling/avklaringsbehov/**,kontrakt/src/main/kotlin/no/nav/aap/behandlingsflyt/kontrakt/avklaringsbehov/**"
description: "Definisjon, AvklaringsbehovKode, løsninger og løsere: publisert kontrakt, registrering og hva løseren gjør"
---
# Regler for avklaringsbehov, løsere og løsninger

- `Definisjon` og `AvklaringsbehovKode` er publisert kontrakt og lagret i DB. Eksisterende verdier endres aldri;
  nytt behov = ny kode + ny `Definisjon` + `@Deprecated` på den gamle om den erstattes. `init`-blokken kaster ved
  duplikat kode, men ingenting stopper en stille endring av `løsesISteg`/`løsesAv` – det må mennesket vurdere.
- Ny løsning: `@JsonTypeName(XXX_KODE)` med string-konstanten fra `AvklaringsbehovKode`, `@JsonIgnoreProperties(ignoreUnknown = true)`,
  `behovstype`-property med `defaultValue`, og klassen må ligge i `løsning/`-pakken (sealed hierarki = registrering).
- Ny løser: `AvklaringsbehovsLøser<XxxLøsning>` i `løser/`-pakken, første konstruktørparameter `RepositoryProvider`
  (reflektiv `AvklaringsbehovsLøserTest` krever det), `forBehov()` må være unik.
- Løsere gjør minst mulig: valider input, lagre `nye ∪ forrige vedtatte` vurderinger på denne behandlingen, returner
  `LøsningsResultat`. Ikke reduser til tidslinje før lagring, ikke endre avklaringsbehov-status, ikke kjør vilkårslogikk.
- Bruk `PeriodisertAvklaringsbehovLøsning` og `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår` som standard;
  "én vurdering per behandling" er unntaket.
- Status-logikk: `aktivHistorikk` (ikke `historikk`) respekterer AVBRUTT-grensen. `måLøses()` ekskluderer
  MANUELT_FRIVILLIG med mindre `gradBehov == PÅKREVD` eller behovet er returnert fra beslutter/KS.
- Test: `XxxLøserTest` med in-memory repositories (mal: `AvklarStudentLøserV2`-området), ikke `relaxed` mockk av
  repositories.
