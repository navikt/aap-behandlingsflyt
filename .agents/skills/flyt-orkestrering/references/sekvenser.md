# Spor av scenarioer

Konkrete, kodeverifiserte beskrivelser av hva som skjer, i rekkefølge. Søk etter klasse- og metodenavnene.
«Jobb X» = motor-jobb av typen X (se motor-jobber-skillen). Stegnumrene viser til flyten for Revurdering/Førstegangsbehandling
(`flyt.py --flyt Revurdering`).

## A. Ny søknad → første stopp

1. Postmottak kaller `POST /api/sak/finnEllerOpprett` (app-rolle `opprett-sak`) → `PersonOgSakService.finnEllerOpprett` oppretter
   person + sak (saksnummer, rettighetsperiode fra søknadsdato).
2. `POST /api/hendelse/send` (eller `/api/hendelse/sak/{saksnummer}/send`) → `MottattHendelseService.registrerMottattHendelse`:
   hopp over hvis (sak, type, referanse) allerede er mottatt → legg jobben `hendelse.håndterer` i kø (`forSak`, prioritet etter type).
3. Jobben `hendelse.håndterer` (`HendelseMottattHåndteringJobbUtfører`): lås sak → dedupliser på nytt → lagre `mottatt_dokument`
   → `HåndterMottattDokumentService.håndterMottatteDokumenter`:
   `MottattHendelseUtleder` gir vurderingsbehov MOTTATT_SØKNAD + årsak SØKNAD →
   `BehandlingService.finnEllerOpprettBehandling` → ingen ytelsesbehandling → ny Førstegangsbehandling (status OPPRETTET, ingen
   steg_historikk) → `SakService.oppdaterRettighetsperioden` (fom = min(fom, mottatt dato), tom = `Tid.MAKS`) → koble dokumentet →
   `ProsesserBehandlingService.triggProsesserBehandling` → jobben `flyt.prosesserBehandling`.
4. Jobben `flyt.prosesserBehandling`: forbered (sak → UTREDES; ingenting å oppdatere før steg 1) → prosesser fra #1
   START_BEHANDLING (OPPDATER_FAKTAGRUNNLAG henter Søknad + Barn; `StartBehandlingSteg` legger til IKKE_VURDERT-perioder for alle
   obligatoriske vilkår) → #2 KRAV … hvert steg: START, OPPDATER_FAKTAGRUNNLAG (egne krav, for eksempel #8 Personopplysning/Lovvalg),
   UTFØRER, AVKLARINGSPUNKT, AVSLUTTER.
5. Første steg som lar et behov som må løses, stå åpent for seg selv (ofte #13 AVKLAR_SYKDOM, tidligere hvis for eksempel lovvalg krever en manuell
   vurdering) → stopp ved AVKLARINGSPUNKT → `BehandlingHendelseService.stoppet` legger `flyt.hendelse` i kø (oppgave oppretter en
   oppgave), sammen med jobbene `flyt.statistikk`, datadeling og meldekort. Transaksjonen fullføres.

## B. Saksbehandler løser et avklaringsbehov

1. Frontend leser `GET /api/behandling/{ref}/flyt` (inkluderer `behandlingVersjon`) og grunnlags-endepunktet for steget.
2. `POST /api/behandling/løs-behov` (eller `/løs-periodisert-behov`) `{referanse, behandlingVersjon, behov: {behovstype: "5003", …}}`.
   I én transaksjon (`AvklaringsbehovApi`):
   - lås sak + behandling; `BehandlingTilstandValidator`: versjonen må være den gjeldende (`OutdatedBehandlingException`), og det kan ikke finnes en
     KLAR/FEILET-jobb av typen `flyt.prosesserBehandling` (`BehandlingUnderProsesseringException`).
    - `AvklaringsbehovOrkestrator.løsAvklaringsbehovOgFortsettProsessering`:
      a. `validerTilstand(behandling, definisjon)`: status er ikke AVSLUTTET; behovet finnes (frivillig/overstyring er unntatt); steget ligger ikke
         etter det aktive steget (ventebehov er unntatt); behov for et tidligere steg er ikke tillatt når det aktive steget er etter
         `sluttÅOppdatereFaktagrunnlag()` (kodene 9001, 9002, 9003, 5050, 5051, 5041 er unntatt).
      b. periodisert løsning → `AvklaringsbehovValidering.validerPerioder`.
      c. `FlytOrkestrator.forberedLøsingAvBehov`: opprett eldre frivillig-/overstyringsbehov hvis det mangler; **gå tilbake til steget for behovet
         hvis behandlingen har passert det**; krev deretter at aktivt steg == dette steget (ventebehov er unntatt).
      d. `løsning.løs(...)` → løseren validerer og lagrer vurderingen → `LøsningsResultat(begrunnelse, kreverToTrinn?)`.
      e. `Avklaringsbehovene.løsAvklaringsbehov` → status AVSLUTTET (+ kreverToTrinn).
      f. legg `flyt.prosesserBehandling` i kø; slett mellomlagret vurdering.
    - `verifiserSkrivelås` øker `versjon` → HTTP 202.
3. Jobben kjører: fortsetter steget fra START → `AvklaringsbehovService` finner en tilstrekkelig løsning → behovet beholder status AVSLUTTET →
   flyten fortsetter til neste stopp. Behov i senere steg kan åpnes på nytt hvis inndataene deres er endret.

## C. Retur fra kvalitetssikrer / beslutter

1. Beslutter sender inn `FatteVedtakLøsning` (én vurdering per totrinn-definisjon, `godkjent` + grunner).
   `FatteVedtakLøser`: Hvis beslutteren har løst et av totrinn-behovene, kastes `KanIkkeVurdereEgneVurderingerException`
   (deaktivert av togglen `IngenValidering`, som er PÅ i test-fakes). Hvis noen har `godkjent=false`, får bare disse statusen
   SENDT_TILBAKE_FRA_BESLUTTER. Ellers får alle relevante behov status TOTRINNS_VURDERT. FATTE_VEDTAK får status AVSLUTTET.
2. Jobben: forbered §3.5 finner behovet som ble sendt tilbake (åpnet på nytt) før FATTE_VEDTAK → gå tilbake til steget → prosesser →
   stegets `AvklaringsbehovService` lar behovet stå åpent (status SENDT_TILBAKE_*) → stopp.
3. Saksbehandleren løser behovet på nytt → AVSLUTTET → flyten fortsetter. `KvalitetssikringsSteg` åpner KVALITETSSIKRING på nytt hvis vurderinger
   er endret siden forrige kvalitetssikring (bare faktisk førstegangsbehandling og klage). `FatteVedtakSteg` åpner
   FATTE_VEDTAK på nytt så lenge et totrinn-behov ikke har status TOTRINNS_VURDERT.
Kvalitetssikring (#24, rollen KVALITETSSIKRER) fungerer på samme måte med SENDT_TILBAKE_FRA_KVALITETSSIKRER. Den omfatter bare behov
med `kvalitetssikres = true` (stegene ved Nav-kontor før #24).

## D. Nytt dokument / vurderingsbehov på en åpen behandling

1. For eksempel legeerklæring → jobben `hendelse.håndterer` → `finnEllerOpprettBehandling`: en åpen ytelsesbehandling finnes → legg til
   MOTTATT_LEGEERKLÆRING-rader (tidsstempel nå) med `oppdaterVurderingsbehovOgÅrsak` → legg `flyt.prosesserBehandling` i kø.
   (Hvis behandlingen har status IVERKSETTES/AVSLUTTET, opprettes en ny Revurdering.)
2. Jobben forbereder behandlingen: et åpent BESTILL_LEGEERKLÆRING-ventebehov løses av `LegeerklæringVentebehovEvaluerer` (gå tilbake dit den
   ventet). Deretter følger §3.7b: det nye vurderingsbehovet er nyere enn siste kjøring av registrerte steg → gå tilbake til det tidligste
   registrerte steget (#9 FASTSETT_MELDEPERIODER for de fleste vurderingsbehov, se references/flyter.md).
3. Prosesseringen starter på nytt derfra. Steg registrert for MOTTATT_LEGEERKLÆRING ser behovet i `vurderingsbehovRelevanteForSteg`.
   `AvklaringsbehovService` åpner AVSLUTTET-behovene deres på nytt fordi vurderingsbehovet er nyere enn siste AVSLUTTET-status for behovet
   (`vurderingsbehovetErNyereEnnAvklaringsbehovet`), når steget melder at det trengs (PÅKREVD).

## E. Meldekort på en sak (fasttrack / atomær behandling)

1. `hendelse.håndterer` med bare MOTTATT_MELDEKORT:
   - siste ytelsesbehandling har status IVERKSETTES/AVSLUTTET → ny Revurdering (grunnlag kopieres) → `MåBehandlesAtomært(ny, null)`;
   - åpen Revurdering → ny Revurdering settes inn mellom siste vedtatte behandling og den åpne (`forrigeBehandlingId` := ny)
     → `MåBehandlesAtomært(ny, åpen)`;
   - åpen Førstegangsbehandling → behovet legges bare til der (vanlig behandling, se eksempel D).
2. `ProsesserBehandlingService.kjørAtomærBehandling` i SAMME transaksjon: atomær orkestrator (stopper ved IVERKSETTES, ingen
   savepoints) kjører `ny` gjennom alle stegene. MeldekortInformasjonskrav (#49) henter ubehandlede meldekort.
   VurderingType MELDEKORT gjør at de fleste steg ikke gjør noe. Behandlingen må nå IVERKSETT_VEDTAK uten å stoppe, ellers
   feiler `check(status.erAvsluttet())` og hele transaksjonen `hendelse.håndterer` rulles tilbake (nytt forsøk).
3. Deretter: `flyt.prosesserBehandling` med LAV prioritet for `ny` (IVERKSETT_VEDTAK → utbetalingsjobb, BREV). For `åpen`:
   `tilbakeførEtterAtomærBehandling` (gå tilbake til det tidligste steget der kravets `flettOpplysningerFraAtomærBehandling` returnerer ENDRET, for eksempel
   meldekort) + legg jobben i kø. Hvis det ikke finnes en `åpen` behandling, planlegges jobben `flyt.informasjonskrav` kl. 19:00 eller senere.

## F. Venting (sett på vent) og gjenopptakelse

1. Manuelt: `POST /api/behandling/{ref}/sett-på-vent` → `AvklaringsbehovOrkestrator.settBehandlingPåVent` → behovet
   MANUELT_SATT_PÅ_VENT (VENTEPUNKT, løsesISteg UDEFINERT → funnetISteg = aktivt steg, standardfrist tre uker) → stoppet-hendelse.
   Flyten kjører ikke. Fra et steg: `return FantVentebehov(Ventebehov(definisjon, grunn, frist))` (definisjonen må være et ventebehov,
   fristen må ligge fram i tid) → behovet opprettes i UTFØRER → stopp i stegets AVKLARINGSPUNKT.
2. Andre utløsere kan fortsatt prosessere behandlingen mens den venter. Den stopper da på ventesteget igjen.
3. Gjenopptakelse: den daglige jobben `batch.gjenopptaBehandlinger` (kl. 07:00) legger prosessering i kø for behandlinger med et OPPRETTET-behov der
   fristen ≤ i dag → forbered løser behovet (`FristUtløptVentebehovEvaluerer`) → gå tilbake til ventesteget → fortsett.
   Saksbehandleren kan også løse ventebehovet (`SattPåVentLøsning`), uavhengig av rekkefølgen på stegene.

## G. Et steg kaster et unntak

1. UTFØRER i steg X kaster et unntak (for eksempel tidsavbrudd i en gateway under `hentData`, eller en `IllegalStateException`).
2. `DBTransaction` ruller tilbake til savepointet fra etter OPPDATER_FAKTAGRUNNLAG i X, og committer resten (steg før X,
   radene START/OPPDATER_FAKTAGRUNNLAG for X og hentede registerdata). Motor registrerer en FEILET-historikkrad. Forsøk 2 og 3 starter
   umiddelbart (uten backoff). Etter det tredje mislykkede forsøket får jobben status FEILET.
3. Når jobben har status FEILET, blokkeres jobbgruppen, nye utløsere gjør ingenting, `løs-behov` gir `BehandlingUnderProsesseringException`,
   og `/flyt/prosessering` viser FEILET. Gjenopprett slik: rett feilen → kjør `oppgave.retryFeilede` (07/12/15/20) eller `GET /drift/api/jobb/rekjor/{id}`.
