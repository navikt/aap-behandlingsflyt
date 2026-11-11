---
name: aap-domene
description: Domenekunnskap om AAP (arbeidsavklaringspenger) i Kelvin/behandlingsflyt, med norsk ordliste og engelske betydninger, modellen for sak og behandling, roller (Nav-kontor, NAY, kvalitetssikrer, beslutter), vilkår med hjemmel, rettighetstyper, kjeden fra vilkår via underveis til tilkjent ytelse, vedtak, utbetaling og brev, de eksterne systemene bak hver gateway og semantikken til Tidslinje/Periode. Bruk når en oppgave bruker domenetermer du ikke kjenner, når et forretningskrav skal knyttes til kode, eller når du jobber med periodiserte data. Keywords: AAP, arbeidsavklaringspenger, ordliste, vilkår, rettighetstype, underveis, meldeperiode, tilkjent ytelse, samordning, Tidslinje, Periode, NAY, Kelvin, Arena.
---

# AAP-domene for behandlingsflyt

Rettslig grunnlag: folketrygdloven kapittel 11 (AAP); henvisninger i kode er «§ 11-x». Kelvin er Navs nye AAP-saksbehandlingssystem
(denne appen er kjernen for saksbehandlingen); Arena er det gamle systemet sakene migreres fra (`MIGRERING_FRA_ARENA`).

## 1. Ordliste

| Begrep | Betydning (kodeforankring) |
|---|---|
| sak / saksnummer | a person's AAP case: one person, one `rettighetsperiode`; root of all behandlinger (`Sak`) |
| behandling | one processing of a sak (`Behandling`, `TypeBehandling`); *ytelsesbehandling* = Førstegangsbehandling or Revurdering |
| førstegangsbehandling / revurdering | first assessment / any later re-assessment of the ytelse (same flow) |
| klage, kabal, andreinstans | appeal; Kabal = NAV klageinstans system; `SvarFraAndreinstans` handles its answer |
| oppfølgingsbehandling | follow-up task behandling (`AVKLAR_OPPFØLGING`) |
| aktivitetsplikt (§ 11-7 / § 11-9) | duty to participate; breaches handled in separate behandlinger, effektueres in revurdering (`EFFEKTUER_11_7`) |
| steg / flyt | step / ordered list of steps (`StegType`, `BehandlingFlyt`) |
| avklaringsbehov | something a human must decide or wait for; stops the flow (`Avklaringsbehov`, `Definisjon`) |
| løsning / løser | payload with the human decision / class that validates and stores it |
| vurdering | an assessment (often by a saksbehandler), often periodised (`PeriodisertVurdering`) |
| vurderingsbehov | reason something must be (re)assessed, attached to a behandling (`Vurderingsbehov`); former name "årsak til behandling" |
| årsak til opprettelse | why the behandling was created (`ÅrsakTilOpprettelse`: SØKNAD, MELDEKORT, …) |
| vurderingType | behandling-level category derived from vurderingsbehov (`VurderingType`) |
| informasjonskrav | fetcher that refreshes facts from registers/documents before a step |
| grunnlag / faktagrunnlag | stored facts and vurderinger per behandling |
| vilkår / vilkårsresultat / utfall | legal condition / all conditions of a behandling / OPPFYLT, IKKE_OPPFYLT, IKKE_VURDERT, IKKE_RELEVANT |
| rettighetsperiode | period the sak covers; fom = earliest søknad/mottak date, tom usually `Tid.MAKS` (2999-01-01) |
| rettighetstype | legal basis AAP is granted on per period (`RettighetsType`) |
| meldekort / meldeperiode / meldeplikt | 14-day employment report / its period / duty to report |
| underveis / uttak | per-period computation of whether and how much is paid (gradering, kvote, meldeplikt) (`FASTSETT_UTTAK`) |
| beregning / grunnlag / G | income basis for the ytelse / grunnbeløp (G-regulering yearly) |
| tilkjent ytelse | computed payout per period (`BEREGN_TILKJENT_YTELSE`) |
| samordning / refusjonskrav | reduction against other benefits / claims for refund (tjenestepensjon, Nav-kontor sosialhjelp) |
| barnetillegg | child supplement |
| institusjonsopphold / soning | stay in health institution / prison (reduces AAP) |
| vedtak / fatte vedtak / beslutter | decision / approve the decision (second person, `FATTE_VEDTAK`) |
| totrinnskontroll | four-eyes approval by beslutter; kvalitetssikring = quality check by kvalitetssikrer before NAY |
| iverksette | execute the vedtak (utbetaling, notifications, brev) |
| tilbakeføring | rewinding a behandling to an earlier step |
| sett på vent / ventebehov / frist | pause with deadline |
| trukket søknad / avbrutt revurdering | withdrawn application / cancelled revurdering (both end the behandling without vedtak effect) |
| NAY | Nav Arbeid og ytelser (national unit), role SAKSBEHANDLER_NASJONAL |
| Nav-kontor / lokalkontor | local office, role SAKSBEHANDLER_OPPFOLGING |
| postmottak | upstream app that journalfører documents and sends them to us |

## 2. Grunnleggende om modellen

- `Sak` 1 — n `Behandling`. Ytelsesbehandlinger danner en enkeltlenket liste via `forrigeBehandlingId` (en revurdering peker på
  behandlingen den bygger på; en fasttrack-revurdering kan settes inn i midten). Klage/SvarFraAndreinstans/
  OppfølgingsBehandling har `forrigeBehandlingId = null`.
- `TypeBehandling` og *faktisk* type: En revurdering etter en behandling uten rett regnes som en førstegangsbehandling
  ved kvalitetssikring, behandling hos beslutter og statistikk (`BehandlingService.utledFaktiskBehandlingstype`).
- `Sak.rettighetsperiode` deles av alle behandlinger og kan endres (tidligere søknad, overstyring eller trukket søknad setter en
  ugyldig/fiktiv periode). Perioden lagres ikke per behandling.
- Behandlingsstatus: OPPRETTET → UTREDES → IVERKSETTES → AVSLUTTET, utledet fra det aktive steget. `erAvsluttet()` inkluderer IVERKSETTES.

## 3. Roller (`no.nav.aap.tilgang.Rolle`, håndheves via aap-tilgang)

SAKSBEHANDLER_OPPFOLGING (Nav-kontor: sykdom, bistand, arbeidsevne, overgang uføre/arbeid, refusjon…) → KVALITETSSIKRER →
SAKSBEHANDLER_NASJONAL (NAY: yrkesskade, student, sykepengeerstatning, beregning, medlemskap, samordning, barnetillegg,
foreslå vedtak…) → BESLUTTER (fatte vedtak, vedtaksbrev). `Definisjon.løsesAv` lister rollene for hvert behov; API-endepunkter
bruker `authorizedGet/Post` / `getGrunnlag` med `Operasjon` (SE, SAKSBEHANDLE, DRIFTE, DRIFT_LES) og en `relevanteIdenterResolver`.

## 4. Vilkår (`Vilkårtype`)

| Vilkårtype | Hjemmel | Obligatorisk* |
|---|---|---|
| ALDERSVILKÅRET | § 11-4 | ✓ |
| LOVVALG | lovvalg/medlemskap | ✓ |
| MEDLEMSKAP | § 11-2 (forutgående medlemskap) | ✓ |
| OPPHOLDSKRAV | § 11-3 | |
| SYKDOMSVILKÅRET | § 11-5 | ✓ |
| BISTANDSVILKÅRET | § 11-6 | ✓ |
| AKTIVITETSPLIKT | § 11-7 | |
| SYKEPENGEERSTATNING | § 11-13 | |
| STUDENT | § 11-14 | |
| OVERGANGARBEIDVILKÅRET | § 11-17 | ✓ |
| OVERGANGUFØREVILKÅRET | § 11-18 | ✓ |
| GRUNNLAGET | § 11-19 | ✓ |
| INNTEKTSBORTFALL | § 11-4 2. ledd | ✓ |
| STRAFFEGJENNOMFØRING | § 11-26 | |
| SAMORDNING / SAMORDNING_ANNEN_LOVGIVNING | § 11-27 / § 11-29 | |
| ORDINÆR_KVOTE / SYKEPENGEERSTATNING_KVOTE | § 11-12 / § 11-13 1. ledd | ✓ |

*`obligatorisk` = `StartBehandlingSteg` oppretter IKKE_VURDERT-perioder for vilkåret i en førstegangsbehandling (teknisk, ikke juridisk).
Vilkårsresultater er periodisert (`Tidslinje<Vilkårsvurdering>`); `Vilkårsvurderer.vurder` må være ren.
`TidligereVurderinger` kombinerer resultatene fra stegene før et gitt steg for å avgjøre «uunngåelig avslag» / «ingen behandlingsgrunnlag»,
slik at senere steg kan hoppe over arbeidet.

## 5. Rettighetstype (`RettighetsType`)

BISTANDSBEHOV (§ 11-6, ordinær kvote), SYKEPENGEERSTATNING (§ 11-13), STUDENT (§ 11-14, ordinær kvote), ARBEIDSSØKER
(§ 11-17), VURDERES_FOR_UFØRETRYGD (§ 11-18). Utledes per periode i `FASTSETT_RETTIGHETSTYPE` fra vilkårsresultatet, etter
prioritetslisten i `KravForRettighetsType.kt`; se arbeidseksempler (gantt) i `behandlingsflyt/behandlingsflyt.md`.

## 6. Fra vurderinger til utbetaling (rekkefølgen i Revurdering-flyten)

krav/rettighetsperiode → lovvalg → meldeperioder → alder → sykdom-gruppe (Nav-kontor) → kvalitetssikring → yrkesskade/student/
sykepengeerstatning → sykdomsvilkåret → beregning (grunnlag) → inntektsbortfall → medlemskap → oppholdskrav → barnetillegg →
institusjon → samordning → effektuer 11-7 → **rettighetstype** → vedtakslengde → meldeplikt → **underveis** →
**tilkjent ytelse** → simulering → foreslå vedtak → **fatte vedtak** (vedtak lagres) → **iverksett** (jobb
`flyt.iverksettUtbetaling` → aap-utbetal; varsler; datadeling) → **brev** (vedtaksbrev via aap-brev).
Fullstendig liste: `python3 .agents/skills/flyt-orkestrering/scripts/flyt.py --flyt Revurdering`.

## 7. Eksterne systemer (gateways registrert i `repository/.../integrasjon/DefaultGatewayProvider.kt`)

PDL (person, identer, barn), POPP/inntekt + A-inntekt (inntektskomponenten), AAREG/EREG (arbeidsforhold), MEDL (medlemskap),
Inst2 (institusjonsopphold), yrkesskade-registeret, Pesys/uføre, TP (tjenestepensjon), Abakus (sykepenger, foreldrepenger),
dagpenger, tiltakspenger, aap-arenaoppslag (Arena), aap-utbetal (utbetaling, simulering), aap-brev + pdfgenerator (brev),
aap-oppgave (oppgavestyring), aap-statistikk, aap-api-intern/SAM (datadeling, samordning id), aap-meldekort-backend,
dokumentinnhenting (legeerklæring), dokarkiv, Gosys, NOM/NORG (enheter), Kabal, aap-tilgang (PIP), Unleash.
Innkommende Kafka (`api/.../hendelse/kafka`): Kabal, PDL-hendelser, tilbakekreving, sykepengevedtak, institusjonsopphold,
uførevedtak, foreldrepengevedtak. Innkommende dokumenter: `POST /api/hendelse/send` fra postmottak.
Utgående hendelser: `flyt.hendelse`-jobb → aap-oppgave (`BehandlingFlytStoppetHendelse`), statistikk, datadeling.

## 8. Tidslinje / Periode (komponentene `tidslinje`, `verdityper`)

- `Periode(fom, tom)`: hele dager, **begge ender inkludert**; `fom > tom` kaster et unntak. `Tid.MIN` = 0001-01-01, `Tid.MAKS` = 2999-01-01.
- `Tidslinje<T>`: sorterte, ikke-overlappende `Segment(periode, verdi)`; opprettelse med overlapp **kaster et unntak**. Det er tillatt med hull.
- `Tidslinje.map2/map3/…` og `outerJoin` er **outer joins**: lambdaen får null der den ene siden mangler en verdi, og
  resultatet dekker unionen. Bruk `innerJoin`/`leftJoin`/`kryss` når du mener snittet eller venstresiden.
- `map`/`mapValue` beholder segmentgrensene; kall `komprimer()` for å slå sammen like nabosegmenter. `begrensetTil(periode)` klipper;
  `komplement(periode) { … }` fyller hull; `filter`, `segment(dato)`, `helePerioden()`, `erSammenhengende()`.
- Opprett med `tidslinjeOf(periode to verdi, …)` eller `liste.somTidslinje { it.periode }`; tom tidslinje: `Tidslinje.empty()` / `.orEmpty()`.
- Testhjelper: `assertTidslinje` i `lib-test/.../help/AssertTidslinje.kt`.
