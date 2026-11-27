# Analyse: lovvalg og medlemskap krever ikke ny manuell vurdering ved nytt krav

Dato: 2026-11-24. Gjelder `VurderLovvalgSteg` (11-2, `AVKLAR_LOVVALG_MEDLEMSKAP`) og delvis
`VurderForutgåendeMedlemskapSteg` (11-2 forutgående, `AVKLAR_FORUTGÅENDE_MEDLEMSKAP`).

## Kort svar

Lovvalgsteget regner en periode som tilstrekkelig vurdert så lenge det finnes **en** manuell vurdering som dekker
den. Manuelle lovvalgsvurderinger er åpne bakover og framover (`tom = null`), og de kopieres inn i den nye
behandlingen. Den gamle vurderingen dekker derfor også perioden til et nytt krav.

Andre periodiserte steg (sykdom, bistand, studentV2 m.fl.) sjekker i tillegg om hvert nytt krav har en vurdering
med `fom` inne i kravperioden (`AvklaringsbehovValidering.nårKravHarLøsning`). Lovvalgsteget går utenom denne
sjekken. Grunnen er at steget sender inn sin egen `perioderSomIkkeErTilstrekkeligVurdert`, og at det ikke sender
`gjeldendeVurderinger` til `AvklaringsbehovService`.

Valideringen når saksbehandler sender inn løsningen (`AvklaringsbehovValidering.validerPerioder`) er i dag det
eneste stedet kravregelen håndheves for lovvalg. Den virker bare når toggelen `NyttKravPeriodiserteAvklaringsbehov`
er på, og bare når steget allerede har åpnet behovet. Med overstyring (`MANUELL_OVERSTYRING_LOVVALG`) hoppes den
over helt. Se [Validering når løsningen sendes inn](#validering-når-løsningen-sendes-inn).

## Hvordan det henger sammen

### 1. Steget bruker egen tilstrekkelighetssjekk som ikke kjenner til krav

`VurderLovvalgSteg.utfør` kaller `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert`
uten `gjeldendeVurderinger`:

```kotlin
// VurderLovvalgSteg.kt:81
avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
    kontekst = kontekst,
    definisjon = Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP,
    tvingerAvklaringsbehov = tvingerAvklaringsbehov,
    nårVurderingErRelevant = ::nårVurderingErRelevant,
    perioderSomIkkeErTilstrekkeligVurdert = ::perioderSomIkkeErTilstrekkeligVurdert,
    tilbakestillGrunnlag = { tilbakestillVurderinger(kontekst) },
)
```

Tilstrekkelighetssjekken ser bare på om det finnes en manuell vurdering eller en automatisk vurdering:

```kotlin
// VurderLovvalgSteg.kt:196
return Tidslinje.map3(relevantTidslinje, manuelleVurderinger, kanVurderesAutomatiskTidslinje) { relevant, manuellVurdering, automatiskVurdering ->
    relevant == true && manuellVurdering == null && automatiskVurdering != true
}.filter { it.verdi }.perioder().toSet()
```

I `AvklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår` (ca. linje 386) brukes en
eksplisitt `perioderSomIkkeErTilstrekkeligVurdert` når den ikke er `null`. Grenen som kombinerer
`nårVurderingErGyldig` med `avklaringsbehovValidering.nårKravHarLøsning(...)` kjøres bare når funksjonen
returnerer `null`. Lovvalg kommer aldri inn i den grenen.

### 2. Gamle manuelle vurderinger dekker nye krav

`ManuellVurderingForLovvalgMedlemskap` har `tom: LocalDate? = null`, og `gjeldendeVurderinger()` gir en tidslinje
som varer til `Tid.MAKS`. `GrunnlagKopierer` kopierer vurderingene inn i den nye behandlingen. En vurdering fra
førstegangsbehandlingen dekker derfor også perioden til et nytt krav (`NY_STØNADSPERIODE` eller
`GJENINNTREDEN_ETTER_OPPHØR`), og `manuellVurdering == null` blir aldri sant.

`nårKravHarLøsning` løser dette for de andre stegene. For krav av typen `NY_STØNADSPERIODE` og
`GJENINNTREDEN_ETTER_OPPHØR` (når `måRevurderesEtterOpphør`, som er standard og gjelder lovvalg) krever den
en vurdering der `fom` ligger inne i kravperioden (`harVurderingForKrav`, `AvklaringsbehovValidering.kt` ca.
linje 175).

### 3. `gjeldendeVurderinger` blir ikke sendt inn

Uten `gjeldendeVurderinger` returnerer `nårKravHarLøsning` `Tidslinje.empty()` (TODO-kommentar i
`AvklaringsbehovValidering.kt` ca. linje 113). Steget mister også den automatiske håndteringen i
`perioderSomBehøverVurdering` og `tilbakestillGrunnlag`.

### 4. Konsekvenser for avklaringsbehovet

| Situasjon | Hva skjer i dag |
|---|---|
| Ny revurdering med `MOTTATT_SØKNAD`, og den automatiske vurderingen feiler for en relevant dato | `tvingerAvklaringsbehov` inneholder `MOTTATT_SØKNAD`, så behovet opprettes. `perioderSomIkkeErTilstrekkeligVurdert` er tom, fordi den gamle vurderingen dekker perioden. Saksbehandler ser ingen perioder som mangler vurdering og kan bekrefte den gamle vurderingen uendret. |
| Søknaden kommer inn i en åpen behandling der behovet allerede er løst | Behovet gjenåpnes fordi vurderingsbehovet er nyere. Tilstrekkelighetssjekken sier fortsatt at alt er vurdert. |
| Den automatiske vurderingen er OK på den nye kravdatoen | `nårVurderingErRelevant` er `false` for den nye perioden, fordi `MOTTATT_SØKNAD` bare tvinger relevans når forrige behandling hadde avslag på lovvalg (linje 223–225). Den gamle manuelle vurderingen gjelder videre. Testen `lovvalg oppfylt manuelt i førstegangsbehandling skal fortsatt være gjeldende i revurdering …` krever denne oppførselen i dag. |
| Nytt krav via `VURDER_KRAV` (for eksempel `KORRIGER_SØKNADSDATO`) | `VURDER_KRAV` står ikke i `vurderingsbehovRelevanteForSteg` for `VurderLovvalgSteg` i `Revurdering.kt`. Steget får ingen tvang. |

Valideringen ved løsing (`AvklaringsbehovValidering.validerPerioderMotVedtakBehøverVurderingOgKrav`) kaller
`nårKravHarLøsning` med vurderingene fra løsningen. Den kan derfor avvise en løsning uten vurdering for det nye
kravet, men bare når toggelen `NyttKravPeriodiserteAvklaringsbehov` er på. Den kan heller ikke åpne et behov
som steget ikke har åpnet.

### 5. Toggle

Både `nårEndringIKrav` (`AvklaringsbehovService.kt` ca. linje 543) og `nårKravHarLøsning` er bak
`NyttKravPeriodiserteAvklaringsbehov`. Med toggelen av fungerer kravmekanismen ikke for noe steg. Feilen i
lovvalg gjelder når toggelen er **på**: da får de andre stegene kravsjekken, men ikke lovvalg.

### 6. Forutgående medlemskap

`VurderForutgåendeMedlemskapSteg` har samme mønster: den sender `gjeldendeVurderinger`, men også en eksplisitt
`perioderSomIkkeErTilstrekkeligVurdert`, så kravsjekken blir hoppet over også her. `MOTTATT_SØKNAD` står heller
ikke i `tvingerAvklaringsbehov`. Fiksen bør gjøres for begge stegene.

## Validering når løsningen sendes inn

### Kallkjeden

`POST /api/behandling/løs-periodisert-behov` (`AvklaringsbehovApi.kt:83`) kjører først
`validerVurderingsperioder` på formen til periodene i requesten, deretter
`AvklaringsbehovOrkestrator.løsAvklaringsbehovOgFortsettProsessering`:

```kotlin
// AvklaringsbehovOrkestrator.kt:55
avklaringsbehovene.validerTilstand(behandling, definisjon)
if (avklaringsbehovLøsning is PeriodisertAvklaringsbehovLøsning<*>) {
    avklaringsbehovValidering.validerPerioder(bruker, avklaringsbehovene, avklaringsbehovLøsning, kontekst)
}
// løses det behov som fremtvinger tilbakehopp?
flytOrkestrator.forberedLøsingAvBehov(definisjon, behandling, kontekst, bruker)
...
avklaringsbehovLøsning.løs(...)   // AvklarPeriodisertLovvalgMedlemskapLøser
```

Både `AvklarPeriodisertLovvalgMedlemskapLøsning` og `AvklarPeriodisertOverstyrtLovvalgMedlemskapLøsning`
implementerer `PeriodisertAvklaringsbehovLøsning`, så begge går gjennom den generelle valideringen.

### Hva den generelle valideringen sjekker

`AvklaringsbehovValidering.validerPerioderMotVedtakBehøverVurderingOgKrav` gjør to sjekker. Begge bruker
`gjeldendeTidslinje = (nye vurderinger fra løsningen + vurderinger fra forrige behandling).gjeldendeVurderinger()`.

| Sjekk | Grunnlag fra avklaringsbehovet | Hva den fanger for lovvalg ved nytt krav |
|---|---|---|
| `perioderSomManglerLøsning` | `perioderVedtaketBehøverVurdering` | Ingenting. Den åpne vurderingen fra forrige behandling (`tom = null`) dekker alle perioder. Dette er tilsiktet, se testen `Periodisert løsning skal validere OK om periodene avklaringsbehovet ber om er dekket av tidligere vurderinger`. |
| `perioderKravManglerLøsning` | `perioderVilkåretErRelevant` (eller `perioderVedtaketBehøverVurdering`) | Avviser løsningen hvis kravperioden ikke har et segment som starter inne i perioden (`harVurderingForKrav`). Gir feilmeldingen «Mangler vurderinger på eller etter dato … på grunn av krav». Bare når toggelen er på. |

`gjeldendeVurderinger()` grupperer per `vurdertIBehandling`, sorterer på `opprettet` og komprimerer
(`PeriodisertVurdering.kt:23`). En ny vurdering med `fom` lik kravdatoen gir derfor et eget segment som
oppfyller kravet. Sender saksbehandler inn én ny vurdering fra rettighetsperiodens start, overstyrer den hele
tidslinjen. Da starter ingen segmenter inne i kravperioden, og løsningen blir avvist. Saksbehandler må altså dele
vurderingen ved kravdatoen.

Med toggelen på håndhever valideringen altså kravregelen for `AVKLAR_LOVVALG_MEDLEMSKAP`, selv om steget ikke
gjør det. Hullene ligger rundt den.

### Hull 1: Valideringen kjører bare hvis steget har åpnet behovet

Valideringen kjører først når saksbehandler løser behovet. Lukker eller gjenåpner steget ikke behovet, får
valideringen aldri kjøre. Det skjer når:

- Den automatiske vurderingen er OK på den nye kravdatoen. Da er perioden ikke i `perioderVilkåretErRelevant`.
  Dette er i tråd med testen på linje 742.
- Kravet endres via `VURDER_KRAV` (for eksempel `KORRIGER_SØKNADSDATO`) i en åpen behandling der lovvalg allerede
  er løst. `VURDER_KRAV` er ikke relevant for lovvalgsteget, så `vurderingsbehovetErNyereEnnAvklaringsbehovet`
  er falsk. `perioderSomIkkeErTilstrekkeligVurdert` er tom fordi den gamle vurderingen dekker perioden.
  `måLøsesPåNytt` blir dermed falsk, og behovet forblir `AVSLUTTET`.

### Hull 2: Steget og valideringen er uenige

Når behovet er åpnet, er `perioderSomIkkeErTilstrekkeligVurdert` på behovet tom fordi steget ikke kjenner til
krav. Grunnlags-API-et (`LovvalgMedlemskapGrunnlagApi`) sender `behøverVurderinger =
perioderVedtaketBehøverVurdering` til frontend. Den inneholder kravperioden bare med toggelen på
(`nårEndringIKrav`). Saksbehandler får derfor først vite om kravregelen fra en 400-feil ved innsending, ikke fra
skjermbildet, hvis frontend bruker `perioderSomIkkeErTilstrekkeligVurdert` eller ikke markerer kravdatoen.
(Frontend er ikke kontrollert i denne analysen.)

### Hull 3: Overstyring hopper over kravsjekken

`MANUELL_OVERSTYRING_LOVVALG` (`BehovType.OVERSTYR`) har ikke noe avklaringsbehov før saksbehandler løser det.
Behovet opprettes i `FlytOrkestrator.forberedLøsingAvBehov` → `leggTilOverstyringHvisMangler`
(`FlytOrkestrator.kt:341`). Det skjer **etter** `validerPerioder` i orkestratoren:

- Første innsending: `hentBehovForDefinisjon(...)` er `null`, og hele valideringen hoppes over
  (`if (behovForDefinisjon != null)`).
- Senere innsendinger: behovet er opprettet med `perioderVedtaketBehøverVurdering = null` og
  `perioderVilkåretErRelevant = null`. Begge sjekkene får tomme perioder og godkjenner alt.

`AvklarPeriodisertOverstyrtLovvalgMedlemskapLøser` lagrer overstyrte vurderinger i samme repository som den
vanlige løseren. Lovvalgsteget skiller ikke på `overstyrt`, så en overstyrt vurdering dekker også et nytt krav.

### Hull 4: Lovvalgsløsernes egne valideringer kjenner ikke til krav

Begge løserne sjekker bare `validerGyldigVurderinger` (begrunnelse) og `validerGyldigForRettighetsperiode`
(sammenhengende og dekker rettighetsperioden). Kontrollen gjøres på tidligere vurderinger slått sammen med nye,
så en løsning uten nye perioder går gjennom. Dette er greit så lenge den generelle valideringen gjør jobben, men
det betyr at løserne ikke er noe sikkerhetsnett.

### Testdekning

`AvklaringsbehovValideringTest` dekker kravreglene generelt (`NyttKrav er ikke dekket når løsning fom er før
muligRettFra`, `Gjeninntreden etter Opphør …` m.fl.). Ingen test dekker lovvalg spesielt, overstyring eller
kombinasjonen gammel åpen vurdering pluss nytt krav.

## Forslag til fiks

### A. Ta med kravsjekken i stegets tilstrekkelighetssjekk (anbefalt)

Minst mulig endring og samme regel som i de andre stegene:

1. Send inn `gjeldendeVurderinger` fra `VurderLovvalgSteg`:
   ```kotlin
   gjeldendeVurderinger = {
       lovvalgMedlemskapRepository.hentHvisEksisterer(kontekst.behandlingId)
           ?.gjeldendeVurderinger().orEmpty()
   }
   ```
2. Utvid `perioderSomIkkeErTilstrekkeligVurdert` med en fjerde tidslinje fra
   `AvklaringsbehovValidering.nårKravHarLøsning(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP, manuelleVurderinger, kontekst.tilFlytKontekst())`.
   En periode er ikke tilstrekkelig vurdert når den er relevant, den automatiske vurderingen ikke er OK, og
   enten mangler manuell vurdering eller kravet mangler løsning (`dekkerKrav == false`).
   - `AvklaringsbehovValidering` må injiseres i steget (`AvklaringsbehovValidering(repositoryProvider, gatewayProvider)`),
     slik `AvklaringsbehovService` gjør i dag.
   - Behold `automatiskVurdering == true` som unntak, så nye krav med automatisk OK fortsatt går forbi. Da står
     testen på linje 742 seg.
3. Gjør det samme i `VurderForutgåendeMedlemskapSteg`.

Fordeler: følger toggelen `NyttKravPeriodiserteAvklaringsbehov`, rører ikke kontrakten og endrer ikke stegrekkefølgen.
Ulempe: duplikat av logikken i `AvklaringsbehovService`.

### B. Bruk `nårVurderingErGyldig` i stedet for egen tilstrekkelighetssjekk

Bytt til `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(nårVurderingErGyldig = …, gjeldendeVurderinger = …)`,
slik `VurderSykdomSteg` gjør. `nårVurderingErGyldig` blir "manuell vurdering finnes eller automatisk OK".
Servicen legger da selv på `nårKravHarLøsning`.

Fordeler: ingen duplisert kravlogikk, og lovvalg følger samme vei som de fleste periodiserte steg.
Ulempe: `perioderVilkåretErVurdert` for forrige behandling bruker i dag stegets
`perioderSomIkkeErTilstrekkeligVurdert`. Det endrer hvilke perioder som regnes som vurdert, så endringen må
dekkes av flyttestene. Servicen hopper også over `tilbakestillGrunnlag` når alle vurderingene er automatiske,
og det må sjekkes mot lovvalgsdelvurderingene.

### C. Avgrens hvem som eier tvangen ved nytt krav (supplement)

- Legg `VURDER_KRAV` til i `vurderingsbehovRelevanteForSteg` for `VurderLovvalgSteg` (og forutgående) i
  `Revurdering.kt`, så en endret kravdato også kjører steget med tvang. Endringen setter ikke inn eller flytter
  steg, men bør bekreftes av fag.
- Ta stilling til om nytt krav skal tvinge relevans også når den automatiske vurderingen er OK på den nye
  datoen. Dette er et faglig valg: testen på linje 742 sier i dag at den gamle manuelle vurderingen skal
  gjelde. Ønsker fag ny vurdering per krav, må `nårVurderingErRelevant` tvinge relevans for perioder med
  endret krav (samme tidslinje som `nårEndringIKrav`), og testen må endres.

### D. Stram inn valideringen når løsningen sendes inn

Gjøres sammen med A eller B, og bak samme toggle.

1. **Overstyring (hull 3).** Valider `MANUELL_OVERSTYRING_LOVVALG` mot periodene på `AVKLAR_LOVVALG_MEDLEMSKAP`.
   Det kan gjøres ved at `validerPerioderMotVedtakBehøverVurderingOgKrav` slår opp det vanlige behovet når
   definisjonen er en overstyring av et periodisert behov, eller ved å flytte `leggTilOverstyringHvisMangler`
   foran valideringen og gi overstyringsbehovet periodene fra det vanlige behovet. Det første er minst
   inngripende: det endrer ikke hva som lagres på behovet. Fag må bekrefte at overstyring skal følge kravregelen.
2. **Steg og validering bruker samme kilde (hull 2).** Når A eller B er gjort, ligger kravperioden i
   `perioderSomIkkeErTilstrekkeligVurdert`. Da viser skjermbildet hva som mangler, og 400-feilen ved innsending
   blir en sikring, ikke den eneste beskjeden. Vurder å la `LovvalgMedlemskapGrunnlagApi` også sende
   kravdatoene, så frontend kan foreslå å dele vurderingen der. Det endrer `openapi.json` og frontend-typene
   og må avtales med frontend.
3. **Hull 1 løses i steget**, ikke i valideringen. Valideringen kan bare kontrollere behov som er åpne. Se A og C.
4. **Løserne (hull 4)** trenger ingen endring. Kravregelen bør bare ligge ett sted, i `AvklaringsbehovValidering`.

## Tester som må til

- `VurderLovvalgStegTest` (ny, domenetest med `InMemory*`): forrige behandling har en åpen manuell vurdering,
  den nye behandlingen har et krav `NY_STØNADSPERIODE` med ny dato der den automatiske vurderingen feiler,
  og toggelen er på. Forvent at `perioderSomIkkeErTilstrekkeligVurdert` inneholder kravperioden. Samme oppsett
  med toggelen av gir tom mengde. Kjør `utfør` to ganger og forvent samme tilstand.
- Samme test for `VurderForutgåendeMedlemskapSteg`.
- `LovvalgOgMedlemskapFlytTest`: nytt test-case med manuell vurdering i førstegangsbehandling, ny søknad som
  blir nytt krav og toggelen på. Forvent åpent behov med ikke-tomme `perioderSomIkkeErTilstrekkeligVurdert`,
  og at løsing uten vurdering for kravperioden blir avvist. Testene på linje 234 og 742 skal fortsatt bestå.
- `AvklaringsbehovValideringTest`: vedtatt åpen lovvalgsvurdering fra forrige behandling, nytt krav, og en
  løsning med én vurdering fra rettighetsperiodens start. Forvent avvisning. Samme løsning delt ved kravdatoen
  skal godkjennes.
- `AvklaringsbehovValideringTest`: `MANUELL_OVERSTYRING_LOVVALG` uten eksisterende behov og med nytt krav som
  mangler vurdering. Forvent avvisning etter fiks D.1 (i dag godkjennes den).
- Kjør `EnklereFlytOrkestratorTest` og `ForutgåendeMedlemskapFlytTest` hver for seg.

## Åpne spørsmål til fag

1. Skal et nytt krav alltid gi ny manuell lovvalgsvurdering når forrige vurdering var manuell, også når
   registeropplysningene nå tillater automatisk vurdering? Svaret avgjør om forslag C trengs.
2. Gjelder dette også for gjenopptak etter stans (`GJENOPPTAK_ETTER_STANS`)? I dag krever
   `nårKravHarLøsning` bare ny vurdering når stansen gjaldt samme vilkår, og lovvalg har ingen `vurdererVilkår`
   satt i `Definisjon`.
3. Skal en overstyring av lovvalg følge samme kravregel som en vanlig vurdering?

## Begrensninger i analysen

Analysen bygger på lesing av koden. Ingen test er kjørt for å gjenskape feilen. Linjenumre gjelder `main` per
2026-11-24 (siste endring i steget: `a125293271`).
