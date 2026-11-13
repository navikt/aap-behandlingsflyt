---
name: feilsok-behandling
description: Feilsøk en behandling eller jobb som står fast, feiler eller oppfører seg uventet i dev/prod eller lokalt. Inneholder en tabell med symptom → årsak, nøyaktige feilmeldinger med forklaring, skrivebeskyttet SQL for å finne ut hva som skjedde (steg_historikk, avklaringsbehov, vurderingsbehov, jobb), endepunkter i drift-API-et og motor-API-et for ny kjøring og trygge manuelle rettinger. Bruk ved «behandlingen henger», «prosesseringen feilet», «kan ikke løse avklaringsbehov», uventet tilbakeføring eller gjenåpnet behov, eller jobbfeil. Nøkkelord — feilsøking, drift, henger, feilet jobb, BehandlingUnderProsesseringException, OutdatedBehandlingException, rekjør, kjør fra steg, playbook.
---

# Feilsøking av behandlinger og jobber

Skrivebeskyttet SQL: [references/sql.md](references/sql.md). Tidligere hendelser og et eksempel på masseprosessering: `docs/playbook.md`.
Mekanikk: flyt-orkestrering (motor, tilbakeføringer), motor-jobber (jobber, nye forsøk), avklaringsbehov (livssyklus for behov).
Produksjonsdata er personopplysninger: bruk helst ID-er (behandlingsreferanse, saksnummer) i stedet for identifikatorer. Ikke lim inn personopplysninger.

## 1. Første gjennomgang (i denne rekkefølgen)

1. Finn behandlingens `referanse` (UUID) → `behandling.id`, `sak_id`, `type`, `status`, `versjon` (sql.md §1).
2. Sjekk jobber for behandlingen/saken: Finnes det en KLAR/FEILET `flyt.prosesserBehandling`? Hvis FEILET, les `jobb_historikk.feilmelding` (§4).
   Sjekk også `hendelse.håndterer` for saken. Innkommende dokumenter behandles per sak i rekkefølge. En FEILET-jobb blokkerer alle senere dokumenter.
3. Finn siste posisjon: nyeste rader i `steg_historikk` (§2), aktivt steg og fase. AVKLARINGSPUNKT betyr at behandlingen venter på et behov;
   andre faser betyr at den stoppet midt i et steg (feil eller fortsatt kjøring).
4. Finn årsaken til at den stoppet: åpne avklaringsbehov (§3). Er et ventebehov forfalt? Har et behov `løsesISteg` før det aktive steget?
5. Finn årsaken til tilbakeføringen: tidsstempler for vurderingsbehov (§5), TILBAKEFØRT-rader (§2), endringer i informasjonskrav (§6), retur
   (`SENDT_TILBAKE_*` i §3).

## 2. Symptom → sannsynlig årsak

| Symptom | Sannsynlig årsak / sjekk |
|---|---|
| UI viser «prosesserer…» uten ende / FEILET | KLAR- eller FEILET-jobb av typen `flyt.prosesserBehandling` (jobber §4). FEILET betyr at 3 raske forsøk feilet. Nytt forsøk kjøres kl. 07/12/15/20 |
| Behandling står fast, uten åpne behov eller jobb | Ingenting utløste kjøringen (for eksempel gjorde en utløser ingenting fordi en jobb fantes, og den jobben feilet og ble avbrutt). Kjør på nytt via drift (§4) |
| Behandlingen venter selv om fristen er passert | `batch.gjenopptaBehandlinger` kjører daglig kl. 07:00, jobben feilet, eller et annet ventebehov fortsatt er åpent |
| Dokument mottatt, men ingenting skjedde | `hendelse.håndterer`-jobb med status FEILET/KLAR for saken; duplikatkontroll (samme referanse finnes allerede i `mottatt_dokument`); NY_ÅRSAK på en avsluttet behandling ignoreres (WARN) |
| Saksbehandlerens vurdering «forsvant» | Steget avgjorde at vedtaket ikke lenger trenger vurderingen → behovet fikk status AVBRUTT + `tilbakestillGrunnlag` (avklaringsbehov-skillen §3). Skjer ofte etter at en tilbakeføring endret tidligere svar |
| Behov åpnet på nytt uten at noen har endret det | Et nytt vurderingsbehov er nyere enn behovets siste AVSLUTTET (dokument, manuell revurderingsårsak), eller `erTilstrekkeligVurdert` ble usann |
| Behandlingen hoppet mange steg tilbake | Nytt eller fornyet vurderingsbehov → tilbakeføring til det tidligste registrerte steget (ofte #9 FASTSETT_MELDEPERIODER); informasjonskrav ENDRET; retur fra beslutter/kvalitetssikrer |
| En revurdering til dukket opp | Den åpne behandlingen hadde status IVERKSETTES (telles som avsluttet), eller en atomisk fasttrack-revurdering (meldekort osv.) ble satt inn før den åpne behandlingen |
| Meldekort-/hendelsesjobb feiler gjentatte ganger med «må kjøres atomært» | Den atomiske revurderingen stoppet ved et avklaringsbehov. Finn steget/behovet i loggene for jobben |

## 3. Feilmeldinger

| Melding (søk etter den) | Betydning |
|---|---|
| `Behandlingen har prosesseringsjobber som venter eller har feilet` (BehandlingUnderProsesseringException) | KLAR/FEILET `flyt.prosesserBehandling` finnes; løs-behov ble avvist |
| `Behandlingen har blitt oppdatert i bakgrunnen` (OutdatedBehandlingException) | `behandlingVersjon` fra UI-et ≠ `behandling.versjon`; last inn på nytt |
| `Behandlingen er avsluttet og kan ikke lenger endres` | AVSLUTTET (kastes også hvis en prosesseringsjobb kjører på en AVSLUTTET behandling) |
| `Forsøker å løse avklaringsbehov '…' som ikke hører til behandlingen` | behovet finnes ikke (steget er ikke nådd ennå eller behovet ble aldri opprettet) |
| `Aktivt steg X må løses før du kan løse Y` | steget til Y kommer etter det aktive steget |
| `Forsøker å løse avklaringsbehov … definert i et steg før nåværende steg` | det aktive steget kommer etter `sluttÅOppdatereFaktagrunnlag()` (FATTE_VEDTAK og senere) |
| `Har uhåndterte behov som skulle vært håndtert før nåværende steg` | et steg opprettet et behov for et TIDLIGERE steg, eller stegrekkefølgen ble endret (IllegalStateException) |
| `…har passert det åpne avklaringsbehovet … Med mindre det har skjedd en endring i rekkefølgen av stegene, så er dette en bug` | ERROR-logg i forbered, etterfulgt av en tilbakeføring; undersøk hvorfor behovet havnet bak |
| `Aktivt steg X er ikke lik det forventede steget Y` | ved løsning: tilbakeføringen til steget for behovet førte ikke dit (for eksempel kan en behandling med status IVERKSETTES ikke tilbakeføres) |
| `Steg X finnes ikke i flyten` | lagret StegType finnes ikke lenger i flyten (steget er fjernet eller har fått nytt navn) |
| `Behandlingen er avsluttet, men det finnes avklaringsbehov som må løses` | flyten ble fullført mens et måLøses-behov fortsatt var åpent |
| `må kjøres atomært, men er ikke i avsluttet tilstand` | fasttrack-behandlingen stoppet ved et behov |
| `Prøvde å tilbakeføre avsluttet eller iverksatt behandling` (WARN) | tilbakeføring ignorert for IVERKSETTES/AVSLUTTET |
| `Kan ikke vurdere kvalitet/to-trinn på egne vurderinger.` | fireøyneprinsippet (KanIkkeVurdereEgneVurderingerException) |
| `Feil under prosessering av jobb` / `Feil under plukking av jobber` | motor; stack trace i sikrede logger og `jobb_historikk.feilmelding` |
| `Kun én jobb skal bli markert …` | konkurranse i motorens bokføring; jobben kjørte, men statusoppdateringen feilet (minst én gang) |

## 4. Verktøy for å utføre handlinger (krever DRIFTE / teamroller i prod; bruk med varsomhet)

behandlingsflyt drift-API (`api/.../drift/DriftApi.kt`, alt under `/api/drift`):
- `POST /behandling/{referanse}/prosesser` body `{"skalForberede": true}` — kjører forbered+prosesser synkront i forespørselen
  (med savepoints). `false` hopper over forbered (ingen tilbakeføringer/informasjonskrav).
- `POST /behandling/{referanse}/kjor-fra-steg` body `{"steg": "AVKLAR_SYKDOM"}` — bare for UTREDES-behandlinger, og bare til et tidligere
  UTREDES-steg i flyten; setter det aktive steget direkte til START (ingen TILBAKEFØRT-rader) og prosesserer.
- Skrivebeskyttede endepunkter: `/sak/{saksnummer}/info`, `/sak/{saksnummer}/mottatte-dokumenter`, `/behandling/{referanse}/vilkår`,
  `/tidligere-vurderinger`, `/tilkjent-ytelse`, `/rettighetsinfo`, `/person`; `/brev/{ref}/avbryt` avbryter en bestilling av vedtaksbrev.
motor-api (`/drift/api/jobb`): `GET /feilende`, `/planlagte-jobber`, `/sisteKjørte`, `GET /rekjor/{jobbId}`,
`/rekjorAlleFeilede`, `/avbryt/{jobbId}`, `/avbrytAlleFeilede`.
Lokalt: `curl -s -XPOST http://localhost:8081/token | jq -r .access_token` gir et token for TestApp (README).

Manuelle SQL-endringer er siste utvei: Når du legger jobber i kø på nytt, sett inn med `kjorbar = false` (sql.md §8) og spre belastningen
(hendelsen i 2026-10 i `docs/playbook.md` opprettet rundt 1000 jobber samtidig med `kjorbar = true` og gjorde Kelvin tregere).
Det er risikabelt å rette data ved å redigere child-rader, fordi radene deles mellom behandlinger (repository-og-migrering §2).
