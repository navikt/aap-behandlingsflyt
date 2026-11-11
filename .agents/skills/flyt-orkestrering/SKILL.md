---
name: flyt-orkestrering
description: Slik fungerer tilstandsmaskinen for behandlingsflyt: FlytOrkestrator, StegOrkestrator, BehandlingFlyt, fasene i StegStatus, når steg kjører, stopper eller kjører på nytt, tilbakeføring (rewind), informasjonskrav, vurderingsbehov/VurderingType, atomære (fasttrack) behandlinger og hvordan feil håndteres med savepoint. Bruk når du lurer på «når/hvorfor kjører steg X», «hvorfor gikk behandlingen tilbake/stoppet», når du endrer flyt eller rekkefølge på steg, eller når du skriver stegløsning. Nøkkelord: flyt, steg, prosesser, forbered, tilbakefør, orkestrator, stegstatus, avklaringspunkt.
---

# Flytmotor: FlytOrkestrator / StegOrkestrator / BehandlingFlyt

Kilde (≈1000 linjer, les hvis du er i tvil): `behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/`
`flyt/FlytOrkestrator.kt`, `flyt/steg/StegOrkestrator.kt`, `flyt/BehandlingFlyt.kt`,
`periodisering/FlytKontekstMedPeriodeService.kt`, `faktagrunnlag/InformasjonskravGrunnlagImpl.kt`.

Verktøy (krever ikke bygging):
- `python3 .agents/skills/flyt-orkestrering/scripts/flyt.py --flyt Revurdering [--vb]` viser steg, klasse, status, informasjonskrav og avklaringsbehov for hvert steg
- `flyt.py --steg AVKLAR_SYKDOM` (eller klassenavn) · `flyt.py --vurderingsbehov X` · `flyt.py --vurderingtype`
- [references/flyter.md](references/flyter.md): genererte tabeller over alle flyter og tilbakeføringsmål for vurderingsbehov
- [references/sekvenser.md](references/sekvenser.md): spor steg for steg gjennom de sju viktigste scenarioene (søknad, løs behov, retur fra beslutter, nytt dokument, meldekort/fasttrack, vent, feil)

## 1. Delene i motoren

| Del | Rolle |
|---|---|
| `BehandlingFlyt` | Uforanderlig, ordnet liste med `Behandlingsflytsteg(steg, kravliste, oppdaterFaktagrunnlag)` og en flyttbar markør `aktivtSteg`. Bygges i `forretningsflyt/behandlingstyper/*.kt` med `BehandlingFlytBuilder`. `TypeBehandling.flyt()` kobler type til flyt. |
| `FlytOrkestrator` | Driver én behandling: `forberedBehandling` (synkroniserer med omverdenen og kan gå tilbake i flyten), deretter `prosesserBehandling` (går gjennom stegene). |
| `StegOrkestrator` | Kjører ett steg gjennom fasene i `StegStatus` og lagrer hver fase i `steg_historikk`. |
| `BehandlingSteg` / `FlytSteg` | Steglogikk (`utfør(kontekst): StegResultat`) / dens `companion object`-fabrikk + `StegType`. |
| `FlytKontekstMedPeriodeService` | Bygger `FlytKontekstMedPerioder` for hvert steg: `vurderingType`, relevante vurderingsbehov og gjeldende `rettighetsperiode`. |
| `InformasjonskravGrunnlagImpl` | Oppdaterer informasjonskrav (registerdata / dokumenter) og rapporterer hvilke som er endret. |
| `AvklaringsbehovOrkestrator` | Startpunkt for å løse avklaringsbehov fra API-et (utenfor løkka). |
| `ProsesserBehandlingJobbUtfører` / `ProsesserBehandlingService` | Motor-jobben `flyt.prosesserBehandling` som kjører motoren / tjenesten som legger jobben i kø eller kjører atomære behandlinger. |

`Førstegangsbehandling.flyt()` returnerer `Revurdering.flyt()`, med 56 identiske steg. Forskjellene kommer fra `kontekst.behandlingType`, `kontekst.vurderingType` og `forrigeBehandlingId`.

## 2. Hva starter motoren

Motoren kjører ikke av seg selv. Den startes fra:
1. Motor-jobben `flyt.prosesserBehandling` → `forberedOgProsesserBehandling(behandlingId)` under en sak- og behandling-radlås (`TaSkriveLåsRepository.lås`). `ProsesserBehandlingService.triggProsesserBehandling` legger jobben i kø. Metoden gjør ingenting hvis det allerede finnes en KLAR- eller FEILET-jobb av denne typen for behandlingen. Kallere: håndtering av dokumenter/hendelser, etter at noen har løst et avklaringsbehov, `batch.gjenopptaBehandlinger` (daglig kl. 07:00, utløpte frister), drift-API-et og etter atomære behandlinger.
2. Synkron atomær kjøring (`ProsesserBehandlingService.kjørAtomærBehandling`, §8) i transaksjonen som opprettet behandlingen.
3. Drift (`Driftfunksjoner.prosesserBehandling` / `kjørFraSteg`, `/api/drift/behandling/{ref}/...`).

Når noen løser et avklaringsbehov via `POST /api/behandling/løs-behov`, kjører ikke løkka. Systemet tilbakefører flyten ved behov, kjører løseren, setter behovet til AVSLUTTET, legger jobben i kø og returnerer 202. Frontend spør jevnlig `GET /api/behandling/{ref}/flyt/prosessering` (JOBBER/FEILET/FERDIG fra ventende jobber).

## 3. Én kjøring = forberedBehandling + prosesserBehandling

`forberedBehandling` (i denne rekkefølgen):
1. `validerTilstand` kaster `UgyldigForespørselException` hvis behandlingen har status AVSLUTTET.
2. Første kjøring (ingen steg_historikk): sakens status → UTREDES.
3. Finnes det et åpent ventebehov? `VentebehovEvaluererService` løser behov med frist ≤ i dag eller der vilkåret er oppfylt (`LegeerklæringVentebehovEvaluerer` for BESTILL_LEGEERKLÆRING). Hvis ingen står åpne, går flyten tilbake til steget der behovene ble løst.
4. Behandlingens status er IVERKSETTES/AVSLUTTET → **stopp her** (ingen informasjonskrav eller tilbakeføringer).
5. Hvis et *måLøses* avklaringsbehov ligger før det aktive steget, går flyten tilbake til det tidligste av disse stegene. Dette er forventet etter retur fra beslutter/kvalitetssikrer. Ellers logges ERROR "...har passert det åpne avklaringsbehovet... bug", vanligvis på grunn av endret rekkefølge på stegene.
6. Oppdater informasjonskrav for alle steg **før** det aktive steget (`alleFaktagrunnlagFørGjeldendeSteg`; tomt hvis det aktive steget kommer etter `sluttÅOppdatereFaktagrunnlag()`).
7. Gå tilbake til det tidligste av følgende steg: (a) et steg som har et informasjonskrav som returnerte ENDRET, (b) et steg som er registrert for et vurderingsbehov med `oppdatertTid` etter stegets siste rad i steg_historikk. Steg med status OPPRETTET (START_BEHANDLING…) blir aldri mål for (a) eller (b).

`prosesserBehandling` (forenklet fra kildekoden):
```
gjeldende = aktivt steg            // or the NEXT step if aktivt steg's last status is AVSLUTTER
behandling.status := gjeldende.type().status
loop:
  if gjeldende.status in stoppNårStatus: return          // only atomic runs set this; NO stoppet-hendelse
  kontekst = FlytKontekstMedPeriodeService.utled(..., gjeldende)
  resultat = StegOrkestrator.utfør(gjeldende, ...)        // §4
  validerPlassering: open måLøses avklaringsbehov with løsesISteg BEFORE gjeldende → IllegalStateException
  neste = if (resultat stopped) gjeldende else flyt.neste()
  behandling.status := neste.status (if changed)
  if stopped or neste == null:
     if neste == null: lukkBehandling() → AVSLUTTET (requires last phase AVSLUTTER, no måLøses behov)
     behandlingHendelseService.stoppet(...)              // jobs: oppgave, statistikk, datadeling, meldekort
     return
  gjeldende = neste
```
Behandlingens status følger `StegType.status` for det aktive steget (OPPRETTET → UTREDES → IVERKSETTES). Statusen AVSLUTTET settes bare via `lukkBehandling`.

## 4. Fasene i ett steg: StegStatus

`StegStatus.rekkefølge()` = START → OPPDATER_FAKTAGRUNNLAG → UTFØRER → AVKLARINGSPUNKT → AVSLUTTER. (KDoc-en i `StegOrkestrator` utelater OPPDATER_FAKTAGRUNNLAG, enumen viser den faktiske rekkefølgen.)

| Fase | Hva skjer | Kan stoppe? |
|---|---|---|
| START | markør; **savepoint** | nei |
| OPPDATER_FAKTAGRUNNLAG | oppdater informasjonskrav som er registrert på dette steget; resultatet ignoreres (ingen tilbakeføring); **savepoint** | nei |
| UTFØRER | `BehandlingSteg.utfør(kontekst)`; `FantVentebehov` legger til ventebehovet (`funnetISteg` = dette steget) og fortsetter | nei |
| AVKLARINGSPUNKT | stopp hvis et avklaringsbehov oppfyller `skalStoppeHer(thisStep)`: `løsesISteg == thisStep` (eller `funnetISteg` hvis UDEFINERT) **og** `måLøses()` | **ja, det eneste stoppunktet** |
| AVSLUTTER | markør | nei |

- Hver fase setter inn en rad i `steg_historikk` (bare den nyeste raden per behandling har `aktiv = true`).
- En behandling som stoppet ved AVKLARINGSPUNKT, starter **samme steg fra START** neste gang → `utfør` kjører igjen.
- Steg kan ikke stoppe flyten direkte. `Fullført` betyr bare at «utfør er ferdig». Systemet avgjør om det skal stoppe ut fra avklaringsbehov i AVKLARINGSPUNKT. Et steg som oppretter et behov for et *senere* steg, fortsetter. Hvis behovet gjelder et *tidligere* steg, kaster `validerPlassering` et unntak.
- `TILBAKEFØRT` er ikke en fase, men skrives ved tilbakeføring.

## 5. Tilbakeføring (rewind): hva det er og ikke er

`FlytOrkestrator.tilbakefør` går bakover fra det aktive steget til målsteget (inkludert) og skriver en TILBAKEFØRT-rad for hvert steg. **Metoden kjører ingen stegkode, sletter ingenting og endrer ikke avklaringsbehov eller grunnlag.** Data avstemmes når stegene kjører framover igjen (`AvklaringsbehovService` vurderer hvert behov på nytt: avbryt + `tilbakestillGrunnlag`, åpne på nytt, lukk). Systemet hopper over tilbakeføring for behandlinger med status IVERKSETTES/AVSLUTTET (warn "Prøvde å tilbakeføre avsluttet eller iverksatt behandling").

| Utløser | Hvor | Mål |
|---|---|---|
| ventebehov løst (fristen utløpt / dokument mottatt) | forbered §3.3 | steget der behandlingen ventet |
| åpent behov før aktivt steg (SENDT_TILBAKE_FRA_BESLUTTER/_KVALITETSSIKRER, åpnet på nytt) | forbered §3.5 | det tidligste av disse stegene |
| informasjonskrav ENDRET for et steg før det aktive steget | forbered §3.7a | det tidligste steget som eier kravet |
| vurderingsbehov lagt til/fornyet etter at et registrert steg sist kjørte | forbered §3.7b | det tidligste registrerte steget, vanligvis #9 FASTSETT_MELDEPERIODER (registrert for `Vurderingsbehov.alle()`); se references/flyter.md |
| saksbehandler løser et behov som tilhører et tidligere steg | `AvklaringsbehovOrkestrator` → `forberedLøsingAvBehov` | behovets `løsesISteg` |
| atomær revurdering satt inn før denne åpne behandlingen | `tilbakeførEtterAtomærBehandling` | det tidligste steget der informasjonskravet `flettOpplysningerFraAtomærBehandling` returnerte ENDRET |
| drift «kjør fra steg» | `Driftfunksjoner.kjørFraSteg` | setter det aktive steget direkte til START (ingen TILBAKEFØRT-rader) |

## 6. Kontekst: VurderingType og vurderingsbehov

`FlytKontekstMedPerioder` for hvert steg:
- `vurderingType`: én verdi per behandling, den høyest prioriterte typen som er utledet fra ALLE behandlingens vurderingsbehov (`prioritertType`). `TypeBehandling.Førstegangsbehandling` gir alltid FØRSTEGANGSBEHANDLING, bortsett fra ved MIGERING_FRA_ARENA. Slik finner du mapping og prioritet: `flyt.py --vurderingtype`. Steg bruker en uttømmende `when` uten `else`. Behold denne stilen, så må ny kode ta stilling til nye typer.
- `vurderingsbehovRelevanteForSteg`: behandlingens vurderingsbehov ∩ lista som ble gitt til `medSteg(...)` for dette steget. **For steg etter `sluttÅOppdatereFaktagrunnlag()` gjelder det ALLE vurderingsbehov**, uavhengig av lista som ble oppgitt.
- `rettighetsperiode`: hentes fra `Sak` på dette tidspunktet (den kan endres under behandlingen).
- `erFørstegangsbehandlingEllerRevurdering()` er true for FØRSTEGANGSBEHANDLING, REVURDERING **og** MIGERING_FRA_ARENA.

`vurderingsbehovRelevanteForSteg` i flytbyggeren gjør bare to ting: filtrerer konteksten og styrer tilbakeføring (§5). **Den avgjør aldri om et steg kjører.** Alle steg i flyten kjører hver gang behandlingen passerer dem. Steget må selv avgjøre om det skal gjøre noe, vanligvis ved hjelp av `vurderingType`, `TidligereVurderinger` eller `vurderingsbehovRelevanteForSteg`.

Vurderingsbehov lagres som rader i `vurderingsbehov` (én per tillegg, med tidsstempel). En hendelse på en åpen ytelsesbehandling legger til rader der, uten å opprette en ny behandling, med mindre fasttrack-reglene i §8 gjelder.

## 7. Informasjonskrav

Kontrakten (`faktagrunnlag/Informasjonskrav.kt`) kjøres av `InformasjonskravGrunnlagImpl.oppdaterFaktagrunnlagForKravliste`:
1. `erRelevant(kontekst, steg, sisteOppdatering)`: avgjør om kravet skal kjøres nå. Informasjon om siste kjøring (`informasjonskrav_oppdatert`, per **sak** + navn) gjør det mulig å begrense hvor ofte kravet kjøres, for eksempel med `ikkeKjørtSisteKalenderdag()`. Mange krav hoppes over for VurderingTypes som ikke gjelder ytelse, eller hvis `TidligereVurderinger` allerede gir avslag.
2. `klargjør`: sekvensielt; DB er tillatt, **ingen eksterne kall**.
3. `hentData`: **parallelt på virtual threads; ingen databasetilgang** (`DBConnection` er ikke trådsikker); eksterne kall gjøres her.
4. `oppdater`: sekvensielt; DB, ingen eksterne kall; returnerer ENDRET / IKKE_ENDRET ved å sammenligne med lagret grunnlag.
`SøknadInformasjonskrav` kjøres alltid først og alene (de andre avhenger av den).

Når kjører kravene? Krav for steg før det aktive steget kjøres i `forbered` (ENDRET → tilbakeføring). Kravene for det aktive steget kjøres i fasen OPPDATER_FAKTAGRUNNLAG (ingen tilbakeføring). Krav for steg som ennå ikke er nådd, kjøres når flyten kommer dit. `flettOpplysningerFraAtomærBehandling` (standardverdi IKKE_ENDRET) fletter data fra en nettopp fullført atomær revurdering inn i den åpne behandlingen. Registrer et krav med `medSteg(steg = X, informasjonskrav = listOf(MyKrav), ...)`, der `MyKrav` er `companion object : Informasjonskravkonstruktør` (navnet må være en ny `InformasjonskravNavn`, som lagres).

## 8. Atomære (fasttrack) behandlinger

`BehandlingService.finnEllerOpprettBehandling`: hvis ALLE innkommende vurderingsbehov kan fasttrack-behandles (MOTTATT_MELDEKORT, FRITAK_MELDEPLIKT, FASTSATT_PERIODE_PASSERT, UTVID_VEDTAKSLENGDE, MIGRER_RETTIGHETSPERIODE, EFFEKTUER_AKTIVITETSPLIKT(_11_9), BARNETILLEGG_SATS_REGULERING, G_REGULERING, OVERGANG_UFORE_AUTOMATISK_STANS) og
- siste ytelsesbehandling har status IVERKSETTES/AVSLUTTET → ny Revurdering → `MåBehandlesAtomært(ny, null)`;
- siste behandling er en åpen **Revurdering** → ny Revurdering settes inn *før* den (`forrigeBehandlingId` kobles om, grunnlag kopieres fra siste vedtatte behandling) → `MåBehandlesAtomært(ny, åpen)`;
- siste behandling er en åpen **Førstegangsbehandling** → vurderingsbehovet legges bare til i den (vanlig behandling).

`kjørAtomærBehandling` kjører den nye behandlingen synkront med `stoppNårStatus = {IVERKSETTES, AVSLUTTET}` og **uten savepoints**, i kallerens transaksjon. Deretter kjøres `check(status.erAvsluttet())`. Hvis et steg stopper på et avklaringsbehov, kaster kontrollen et unntak. Hele transaksjonen hos kalleren (for eksempel jobben `hendelse.håndterer`) rulles tilbake og forsøkes på nytt. Fasttrack-løp må derfor gå automatisk helt fram til FATTE_VEDTAK. Etterpå fullfører en vanlig jobb med LAV prioritet IVERKSETT_VEDTAK/BREV. Den åpne behandlingen (hvis den finnes) tilbakeføres via `tilbakeførEtterAtomærBehandling` og legges i kø på nytt. For meldekortlignende behov planlegges en `flyt.informasjonskrav`-jobb kl. 19:00.

## 9. Feilhåndtering og savepoints (ikke opplagt!)

I `flyt.prosesserBehandling`-jobber kaller `StegOrkestrator` `behandlingRepository.markerSavepoint()` etter START og OPPDATER_FAKTAGRUNNLAG i hvert steg. Ved et unntak ruller dbconnects `DBTransaction` tilbake **til siste savepoint**, og slår deretter på autocommit. Det **lagrer alt som skjedde før savepointet**. Fullførte steg, sideeffektene deres (avklaringsbehov og jobber lagt i kø), den nye stegposisjonen og nylig hentede registerdata blir derfor lagret. Motor-jobben markeres som mislykket og forsøkes på nytt (som standard tre forsøk rett etter hverandre). Den starter på nytt fra START i steget som feilet. Atomære kjøringer og `Driftfunksjoner` sin `flytOrkestratorUtenSavepoints` bruker `markSavepointAt = emptySet()` og ruller dermed tilbake alt eller ingenting. Se skillen for motor-jobber for informasjon om nye forsøk og blokkering.

## 10. Invarianter og fallgruver (bekreftet i koden)

- `utfør` må være **idempotent** og avstemme tilstanden ved hver kjøring (ny kjøring ved gjenopptakelse, tilbakeføring og nytt forsøk). Bruk `AvklaringsbehovService` i stedet for å opprette eller lukke behov manuelt.
- Eksterne sideeffekter i steg bør legges i kø som motor-jobber (outbox), for eksempel `IverksettVedtakSteg` → `flyt.iverksettUtbetaling` (`forSak` sørger for at utbetalinger for samme sak kjører i riktig rekkefølge).
- `behandling.flyt()` returnerer en **ny** `BehandlingFlyt` med markøren på FØRSTE steg. Kall `forberedFlyt(behandling.aktivtSteg())` før `neste()`, `aktivtSteg()`, `skalOppdatereFaktagrunnlag()`, `alleFaktagrunnlagFørGjeldendeSteg()`, `gjenståendeStegIAktivGruppe()` eller `tilbakeflyt*`. (Et eksempel på feilen: `BehandlingService.validerStegStatus` kaller `skalOppdatereFaktagrunnlag()` på en ny flyt, så kontrollen kjøres aldri.)
- `Status.erAvsluttet()` (behandling) er sann både for IVERKSETTES og AVSLUTTET. Nye hendelser på en behandling med status IVERKSETTES oppretter en NY revurdering oppå den.
- `TidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())` ser bare på kontroller for steg **før** det angitte steget, og bare for Førstegangsbehandling/Revurdering. `TidligereVurderingerImpl` krever at kontrollene følger rekkefølgen i flyten. Hvis stegene sorteres på nytt, kan opprettelsen feile.
- `stegComparator`/`erStegFør` bruker posisjoner i flyten. En StegType som ikke finnes i flyten, sorteres sist (comparator) eller får indeks −1 (`erStegFør`).
- `Avklaringsbehovene` leser DB på nytt for hvert kall til `alle()`. Objektene du har, er øyeblikksbilder.

## 11. Slik endrer du en flyt trygt

- `StegType`-navn lagres (`steg_historikk.steg`, `avklaringsbehov.funnet_i_steg`) og publiseres i `kontrakt`. Ikke gi nytt navn til eller fjern dem. Behold gamle navn (OPPRETT_REVURDERING, VIS_GRUNNLAG; `VurderStudentSteg` bruker fortsatt AVKLAR_STUDENT, mens AVKLAR_STUDENT_V2 er den nye).
- Hvis du fjerner et steg som åpne behandlinger står på, får du "Steg X finnes ikke i flyten" når de behandles.
- Når du setter inn et steg, får nye behandlinger det. Åpne behandlinger som allerede har passert det, kjører det først etter tilbakeføring til et tidligere steg.
- Hvis du flytter et steg fram eller tilbake, kan åpne behov bli liggende «bak» det aktive steget. Da logges ERROR og flyten går tilbake, eller `validerPlassering` kaster et unntak.
- Etter at du har endret en flyt, kjør `python3 .agents/skills/flyt-orkestrering/scripts/flyt.py --check .agents/skills/flyt-orkestrering/references/flyter.md` (generer på nytt med `--markdown >` hvis fila er utdatert), oppdater `TidligereVurderingerImpl` ved behov, og kjør de nærmeste flyt-testene i små grupper (se testing-skillen; bruk `kjor_test.py --batch flyt` ved større endringer). Oppskrift på nye steg: legg-til-steg-skillen.
