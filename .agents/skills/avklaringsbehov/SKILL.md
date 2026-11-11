---
name: avklaringsbehov
description: Avklaringsbehov fra start til slutt, altså menneskelige beslutningspunkter og ventepunkter. Omfatter Definisjon i kontrakt (BehovType, løsesISteg, kreverToTrinn, kvalitetssikres, løsesAv), statushåndtering, måLøses/skalStoppeHer, avstemmingen i AvklaringsbehovService (opprette/lukke/gjenåpne/avbryte + tilbakestillGrunnlag), periodiserte behov, frivillige behov, overstyring og ventepunkt, kvalitetssikring og beslutter (totrinn), samt løsning via API-et (løsning → løser) og valideringene. Bruk når et behov oppstår, forsvinner eller åpnes på nytt uventet, når du legger til eller endrer en Definisjon, løsning eller løser, eller når du feilsøker retur/kvalitetssikring/beslutter. Keywords: avklaringsbehov, definisjon, løsning, løser, totrinn, beslutter, kvalitetssikring, ventebehov, frivillig, overstyring.
---

# Avklaringsbehov

Kode: `behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/behandling/avklaringsbehov/`
(`Avklaringsbehov.kt`, `Avklaringsbehovene.kt`, `AvklaringsbehovService.kt`, `AvklaringsbehovOrkestrator.kt`,
`ValiderBehandlingTilstand.kt`, `løsning/`, `løser/`) og `kontrakt/.../avklaringsbehov/{Definisjon,Status,AvklaringsbehovKode}.kt`.

Verktøy: `python3 .agents/skills/avklaringsbehov/scripts/avklaringsbehov.py [--kode 5003 | --definisjon X | --steg STEG]`
(kode, type, steg, flagg, roller, Løsning → Løser, hvilke steg som refererer til det) · generert tabell
[references/definisjoner.md](references/definisjoner.md).

## 1. Modell

- **`Definisjon`** (kontrakts-enum, publisert til andre apper): `kode` (4 sifre, `AvklaringsbehovKode`), `type: BehovType`,
  `løsesISteg: StegType` (default UDEFINERT), `kreverToTrinn`, `kvalitetssikres`, `løsesAv: List<Rolle>`, `defaultFrist`
  (ventepunkt), `vurdererVilkår`. `init` validerer at kodene er unike og at reglene for typene er fulgt.
- **BehovType**: MANUELT_PÅKREVD (opprettes bare av systemet); MANUELT_FRIVILLIG (systemet eller saksbehandleren kan opprette det);
  OVERSTYR (saksbehandleren overstyrer en automatisk vurdering; opprettes ved innsending); VENTEPUNKT (vent med frist); BREV og
  BREV_VENTEPUNKT er deprecated. Ventepunktene MANUELT_SATT_PÅ_VENT, BESTILL_LEGEERKLÆRING, OPPRETT_HENDELSE_PÅ_SAK må
  ha `løsesISteg = UDEFINERT` (= løses i steget der behovet oppdages); andre ventepunkter trenger et steg og en `defaultFrist`.
- **`Avklaringsbehov`**: én rad per (behandling, definisjon) (unik indeks) + historikk som bare får nye oppføringer (`avklaringsbehov_endring`:
  status, begrunnelse, endretAv, frist, grunn, perioder, gradBehov, årsak til retur). `status()` = nyeste oppføring.
  `aktivHistorikk` = oppføringer etter siste AVBRUTT. `funnetISteg` = steget som opprettet behovet.
- **`Avklaringsbehovene`**: behandlingens behov; **leser DB på nytt ved hvert oppslag** (`alle()`).

## 2. Statusforløp (kontrakten `Status`)

| Status | Betydning | erÅpent | erAvsluttet |
|---|---|---|---|
| OPPRETTET | trenger eller godtar en løsning | ✓ | |
| AVSLUTTET | løsning mottatt (kan gå videre til KS/totrinn) | | ✓ |
| KVALITETSSIKRET / TOTRINNS_VURDERT | godkjent av kvalitetssikrer / beslutter | | ✓ |
| SENDT_TILBAKE_FRA_KVALITETSSIKRER / _FRA_BESLUTTER | avvist, må behandles på nytt | ✓ | |
| AVBRUTT | ikke lenger relevant (myk sletting) | | ✓ ← pass på: «avsluttet» ≠ «løst» |

- `måLøses()` = åpent og (ikke MANUELT_FRIVILLIG, eller `gradBehov` er null/PÅKREVD, eller sendt tilbake).
- `skalStoppeHer(steg)` = (`løsesISteg == steg`, eller `funnetISteg == steg` når UDEFINERT) og `måLøses()` — evalueres i fasen
  AVKLARINGSPUNKT; det er dette som stopper flyten.
- `harLøsning()` = AVSLUTTET i `aktivHistorikk`. `erTotrinn()` = `definisjon.kreverToTrinn` eller at en løser returnerte
  `LøsningsResultat(kreverToTrinn = true)`.
- Bruk metoden `løsesISteg()` (løser UDEFINERT → funnetISteg), ikke feltet `definisjon.løsesISteg`, når du bestemmer rekkefølgen.

## 3. Avstemmingen alle steg skal bruke: `AvklaringsbehovService`

Steg oppretter eller lukker aldri behov manuelt. De kaller dette ved hver kjøring:
- `oppdaterAvklaringsbehov(definisjon, vedtakBehøverVurdering, erTilstrekkeligVurdert, tilbakestillGrunnlag, kontekst)` (markert deprecated, men mye brukt; oversetter true/false til PÅKREVD/INGEN_BEHOV), eller `oppdaterAvklaringsbehovMedGrad(... behov: () -> Behov ...)`;
- `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(...)` (§4) for periodiserte vilkår.

Beslutningstabell (`oppdaterAvklaringsbehov`, privat overload):

| Trenger vedtaket en menneskelig vurdering? | Eksisterende behov | Har løsning og ikke `måLøsesPåNytt` | Resultat |
|---|---|---|---|
| ja | ingen | – | opprett OPPRETTET (med perioder, gradBehov) |
| ja | OPPRETTET | ja | AVSLUTTET («tilstrekkelig vurdert») |
| ja | SENDT_TILBAKE_* | ja | forblir åpent (et menneske avviste det); perioder oppdatert |
| ja | AVSLUTTET / KVALITETSSIKRET / TOTRINNS_VURDERT | ja | uendret |
| ja | OPPRETTET / SENDT_TILBAKE_* | nei | forblir åpent; perioder oppdatert |
| ja | AVSLUTTET / KS / TOTRINNS / AVBRUTT | nei | **åpnes på nytt** (OPPRETTET) |
| nei | ingen / AVBRUTT | – | ingenting |
| nei | alt annet | – | søknad trukket eller revurdering avbrutt → AVBRUTT; eldre frivillig behov → uendret; ellers **AVBRUTT + `tilbakestillGrunnlag()`** |

`måLøsesPåNytt` = et relevant vurderingsbehov er nyere enn behovets siste avslutningsoppføring, eller `!erTilstrekkeligVurdert()`
(eller `perioderSomIkkeErTilstrekkeligVurdert` ikke er tom).

Kontrakt for lambdaene (fra KDoc, dette må være riktig):
- *vedtakBehøverVurdering / behov* svarer på «vil **vedtaket** inneholde en menneskelig vurdering av dette?», ikke «trenger den
  nåværende tilstanden en menneskelig vurdering nå». Verdien må fortsatt være true etter at saksbehandleren har løst behovet. Ellers
  avbryter neste kjøring behovet og tilbakestiller grunnlaget, slik at saksbehandlerens arbeid forsvinner.
- *erTilstrekkeligVurdert* betyr at den lagrede løsningen er fullstendig og konsistent nok til å fortsette.
- *tilbakestillGrunnlag* tilbakestiller bare dette behovets manuelle vurderinger i denne behandlingen til verdiene fra forrige behandling
  (`kontekst.forrigeBehandlingId`). Ikke endre registerdata eller data fra andre steg. Kalles når behovet avbrytes fordi det ikke lenger
  er relevant (for eksempel etter at en tilbakeføring endret tidligere svar).

## 4. Periodiserte behov (`oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår`)

Parametere: `tvingerAvklaringsbehov` (vurderingsbehov som tvinger fram et behov), `nårVurderingErRelevant(kontekst): Tidslinje<Boolean>`
(også tilgjengelig via `AvklaringsbehovMetadataUtleder` for «ikke relevante perioder» i UI-et), `nårVurderingErGyldig`,
`gjeldendeVurderinger` (`Tidslinje<PeriodisertVurdering>` med `vurdertIBehandling`), `tilbakestillGrunnlag`.
Behovet er PÅKREVD når (bare for vurderingType FØRSTEGANGSBEHANDLING/REVURDERING/MIGERING_FRA_ARENA):
1. minst én periode er relevant, og
2. et relevant vurderingsbehov finnes i `tvingerAvklaringsbehov`, eller en vurdering (frivillig) ble gjort i denne behandlingen, eller
   det finnes perioder som er relevante nå, men som ikke var relevante eller tilstrekkelig vurdert i forrige behandling (eller der
   kravene er endret, toggle `NyttKravPeriodiserteAvklaringsbehov`), minus perioder som ble vurdert automatisk i denne behandlingen.
`perioderVedtaketBehøverVurdering` og `perioderSomIkkeErTilstrekkeligVurdert` lagres på endringen og styrer UI-et.
Referanseimplementasjon: `VurderSykdomSteg`.

## 5. Løsning (API → AvklaringsbehovOrkestrator)

Full gjennomgang: flyt-orkestrering `references/sekvenser.md` §B. Valideringer som avviser en løsning:
- behandling AVSLUTTET; `behandlingVersjon` er utdatert (`OutdatedBehandlingException`); en `flyt.prosesserBehandling`-jobb venter eller har
  status FEILET (`BehandlingUnderProsesseringException`);
- behovet finnes ikke på behandlingen (frivillig behov/overstyring er unntatt); steget ligger etter det aktive steget (ventebehov er unntatt); det tilhører
  et tidligere steg mens det aktive steget er etter `sluttÅOppdatereFaktagrunnlag()` (unntak: 9001, 9002, 9003, 5050, 5051, 5041);
- periodiserte behov: perioder overlapper eller er ugyldige; løser-spesifikke `UgyldigForespørselException`-unntak.
Deretter: gå tilbake til behovets steg hvis nødvendig → løseren lagrer → behovet får status AVSLUTTET → jobb legges i kø → HTTP 202.

## 6. Frivillige behov, overstyring og ventepunkt

- **Eldre automatisk opprettede frivillige behov** (`Definisjon.legacyAutomatiskFrivillgeAvklaringsbehov`): opprettes ved innsending
  (`leggTilFrivilligHvisMangler`); tilbys UI-et av `Avklaringsbehovene.allePlussFrivillige` som **falske objekter med
  id `Long.MAX_VALUE`**; avbrytes aldri av tjenesten når de ikke lenger trengs. AVKLAR_SAMORDNING_SYKESTIPEND legges bare til denne listen
  når togglen `DynamiskFrivilligSykestipend` er av.
- **Dynamiske frivillige behov**: Steget avgjør; PÅKREVD bare hvis `gjeldendeVurderinger` inneholder en vurdering fra denne behandlingen.
  `ikkeEkteFrivillig` (i AvklaringsbehovService) lister definisjoner som noen ganger er frivillige og andre ganger påkrevde.
- **Overstyring** (for eksempel MANUELL_OVERSTYRING_LOVVALG, OVERSTYR_IKKE_OPPFYLT_MELDEPLIKT, AVKLAR_BARNETILLEGG,
  FASTSETT_MANUELL_INNTEKT): opprettes ved innsending (`leggTilOverstyringHvisMangler`).
- **Ventepunkt**: opprettes av `FantVentebehov` fra et steg eller av `sett-på-vent`; løses når fristen ≤ dagens dato
  (`FristUtløptVentebehovEvaluerer`) eller av en `SpesifikkVentebehovEvaluerer` (sealed; nye subklasser med en
  `(RepositoryProvider)`-konstruktør fanges opp ved hjelp av reflection), eller løses manuelt hvor som helst i flyten.

## 7. Kvalitetssikring og beslutter

Rekkefølge for Revurdering/Førstegangsbehandling: Nav-kontor-steg (SAKSBEHANDLER_OPPFOLGING) → #24 KVALITETSSIKRING (KVALITETSSIKRER)
→ NAY-steg (SAKSBEHANDLER_NASJONAL) → #53 FORESLÅ_VEDTAK (NAY) → #54 FATTE_VEDTAK (BESLUTTER) → #55 IVERKSETT_VEDTAK →
#56 BREV (SKRIV_VEDTAKSBREV, BESLUTTER).
- KVALITETSSIKRING trengs bare hvis den *faktiske* behandlingstypen (`BehandlingService.utledFaktiskBehandlingstype`: en revurdering
  der forrige behandling ikke hadde rett, regnes som førstegangsbehandling) er Førstegangsbehandling eller Klage, og et behov som ikke er
  avbrutt har `kvalitetssikres`. Kvalitetssikring kreves på nytt hvis en slik vurdering er endret etter forrige kvalitetssikring (`VurderingEndretService`).
- FORESLÅ_VEDTAK trengs bare hvis det finnes behandlingsgrunnlag og NAY har løst noe (særtilfelle: bare lovvalg etter sykdom → nei).
- FATTE_VEDTAK trengs bare hvis et behov som ikke er avbrutt har `erTotrinn()` (ikke når klagen opprettholdes, søknaden er trukket,
  grunnlag mangler eller togglen `HoppOverBeslutterVedAvslagSykdom` gjelder). `FatteVedtakSteg` lagrer vedtaket (vedtakstidspunkt = siste AVSLUTTET for FATTE_VEDTAK).
- Retur: løseren setter SENDT_TILBAKE_* bare på behovet som ble avvist; neste kjøring går tilbake til det tidligste av disse stegene.
- Fireøyneprinsippet: `KanIkkeVurdereEgneVurderingerException` kastes hvis godkjenneren løste ett av behovene. **Sjekken hoppes over når togglen
  `IngenValidering` er på, slik den er i `LokalUnleash` og `AlleAvskruddUnleash`**, så tester fanger ikke opp brudd.

## 8. Legge til eller endre en Definisjon (sammendrag, full oppskrift i legg-til-steg)

1. kontrakt: ny `const val X_KODE` + enum-verdi i `AvklaringsbehovKode.kt`, ny `Definisjon`-oppføring. Koder og navn lagres
   og brukes av andre apper: Ikke bruk dem på nytt eller gi dem nytt navn; marker dem heller som deprecated. Øk kontraktversjonen i aap-tilgang,
   aap-oppgave og aap-statistikk (se `kontrakt/.../README.md`).
2. `løsning/XLøsning.kt` med `@JsonTypeName(value = X_KODE)` som implementerer `EnkeltAvklaringsbehovLøsning` eller
    `PeriodisertAvklaringsbehovLøsning<…>` (sealed: må ligge i denne pakken) → `løs()` delegerer til `løser/XLøser.kt`
    (`AvklaringsbehovsLøser<XLøsning>`, også sealed).
3. I steget der `StegType == løsesISteg`: kall `AvklaringsbehovService` ved hver kjøring.
4. Grunnlag-endepunkt i `api` (bruk `Definisjon.X.løsesAv` for `påkrevdRolle`), registrer det i `App.kt`.
5. Generer på nytt: `avklaringsbehov.py --markdown > .agents/skills/avklaringsbehov/references/definisjoner.md`.

## 9. Fallgruver

- `Avklaringsbehovene.leggTil` er deprecated for steg: metoden ignorerer et allerede åpent behov uten å varsle og åpner lukkede behov på nytt.
- Avklaringsbehov **kopieres ikke** til en revurdering; tidligere svar finnes i grunnlaget som hentes via `forrigeBehandlingId`.
- `Status.erAvsluttet()` inkluderer AVBRUTT; bruk `harLøsning()` / `status() == AVSLUTTET` når du mener «løst».
- Et steg kan bare opprette behov for seg selv eller senere steg (et tidligere steg gir `IllegalStateException` i `validerPlassering`).
- Hvis `løsesISteg` for en eksisterende Definisjon endres, flyttes åpne behov mellom steg. Behandle dette som en endring i stegrekkefølgen.
