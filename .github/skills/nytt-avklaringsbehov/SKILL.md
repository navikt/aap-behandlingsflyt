---
name: nytt-avklaringsbehov
description: Oppskrift for nytt avklaringsbehov med Definisjon, AvklaringsbehovKode, Løsning, Løser og kobling i steget. Bruk når saksbehandler skal ta stilling til noe nytt, eller noen sier "nytt behov"/"ny løser".
---
# Nytt avklaringsbehov + løser

Les `.github/instructions/avklaringsbehov.instructions.md` først. Reelt eksempel: commit `491c03f9b` (løser/løsning
for avklar stønadsperiode) og den fulle skiven `6d0768796` (avslag 11-27, 44 filer).

1. `kontrakt/.../avklaringsbehov/AvklaringsbehovKode.kt`: ny enum-verdi `` `NNNN` `` **og** `const val XXX_KODE = "NNNN"`.
   Velg neste ledige firesifrede kode i riktig serie.
2. `kontrakt/.../avklaringsbehov/Definisjon.kt`: ny entry med `kode`, `type`, `løsesISteg`, `løsesAv`, `kreverToTrinn`,
   `kvalitetssikres`. Eksisterende entries røres ikke. Kontraktendring – konsumenter må bumpe før bruk.
3. `behandlingsflyt/.../behandling/avklaringsbehov/løsning/XxxLøsning.kt`: `@JsonTypeName(XXX_KODE)`,
   `@JsonIgnoreProperties(ignoreUnknown = true)`, `behovstype` med `defaultValue`, implementerer
   `EnkeltAvklaringsbehovLøsning` eller (helst) `PeriodisertAvklaringsbehovLøsning`; `løs()` delegerer til løseren.
4. `.../løser/XxxLøser.kt`: `AvklaringsbehovsLøser<XxxLøsning>` med `constructor(repositoryProvider: RepositoryProvider, …)`
   som første parameter, `løs()` som validerer og lagrer `nye ∪ forrige vedtatte`, `forBehov()` unik.
5. Steget som eier behovet: `AvklaringsbehovService.oppdaterAvklaringsbehov(definisjon, vedtakBehøverVurdering,
   erTilstrekkeligVurdert, tilbakestillGrunnlag, kontekst)`. Implementer `tilbakestillGrunnlag` ordentlig.
6. Grunnlag-DTO/validering under `faktagrunnlag/saksbehandler/<feature>/` og grunnlag-API i `api/`.
7. Tester: `XxxLøserTest` (in-memory repos), `AvklaringsbehovsLøserTest` plukker opp løseren reflektivt, oppdater
   scenariotest som nå stopper på det nye behovet. Oppdater `DefinisjonTest`-gulltabellen hvis den finnes.
8. Etterarbeid utenfor repoet (nevn i PR): bump kontrakt i aap-oppgave/aap-tilgang/aap-statistikk, generer
   frontend-typer og oversettelse av behovsnavnet i aap-saksbehandling.
