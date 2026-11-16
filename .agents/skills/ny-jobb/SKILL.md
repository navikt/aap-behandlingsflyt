---
name: ny-jobb
description: Oppskrift for ny bakgrunnsjobb (JobbUtfører) i Motor, periodisk eller hendelsesdrevet, med registrering og tester. Bruk når noe skal skje asynkront, på cron, eller for mange saker.
---
# Ny jobb (JobbUtfører)

Reelle eksempler: `ba1d22026` (varsle api-intern om ny søknad, 9 filer); fan-out-mønsteret
`OpprettJobbFor…JobbUtfører` (cron) → `OpprettBehandling…JobbUtfører` (per sak) i `23a6e510b`.

1. `behandlingsflyt/.../prosessering/XxxJobbUtfører.kt`: `class XxxJobbUtfører(deps) : JobbUtfører { utfør(input) }`
   + `companion object : ProvidersJobbSpesifikasjon { navn, type = "flyt.xxx" (stabil streng, lagres i JOBB), beskrivelse,
   konstruer(repositoryProvider, gatewayProvider), evt. cron/retries/retryBackoffTid/prioritet, nyJobb(...) }`.
2. Registrer i `prosessering/ProsesseringsJobber.alle()` (prod-ekskluderte jobber i `if (!Miljø.erProd())`).
3. Legg i kø med `repositoryProvider.provide<FlytJobbRepository>().leggTil(XxxJobbUtfører.nyJobb(...))`.
   Bruk `forSak(...)` for jobber som rører en sak: Motor serialiserer per `(sak, behandling, type)`.
4. Jobber som skriver må ta `lås(sakId[, behandlingId])` først (sak før behandling) og `verifiserSkrivelås` til slutt.
5. Fan-out (én jobb per sak fra en cron-jobb): hopp over saker som allerede har en slik jobb i kø, f.eks.
   `flytJobbRepository.hentJobberForSak(sakId.toLong()).any { it.type() == OpprettBehandlingXxxJobbUtfører.type }`.
   `23a6e510b` manglet dette og fikk duplikate jobber; rettet med test i `71e47469d`.
6. Husk: 3 forsøk, deretter FEILET og gruppen blokkeres til drift griper inn. Idempotens er jobbens ansvar.
7. Tester: `XxxJobbUtførerTest` med in-memory repos (ikke relaxed mockk av repositories); kjør `utfør` to ganger.
   For E2E: `ManuellMotorImpl` i flyttestene kjører køen synkront.
