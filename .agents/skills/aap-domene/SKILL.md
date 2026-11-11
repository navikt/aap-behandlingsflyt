---
name: aap-domene
description: Domain knowledge for AAP (arbeidsavklaringspenger) in Kelvin/behandlingsflyt — Norwegian glossary with English meaning, the sak/behandling model, roles (Nav-kontor, NAY, kvalitetssikrer, beslutter), vilkår with hjemmel, rettighetstyper, the chain from vilkår to underveis, tilkjent ytelse, vedtak, utbetaling and brev, the external systems behind each gateway, and Tidslinje/Periode semantics. Use when a task uses domain terms you don't know, when mapping a business requirement to code, or when working with periodised data. Keywords — AAP, arbeidsavklaringspenger, ordliste, vilkår, rettighetstype, underveis, meldeperiode, tilkjent ytelse, samordning, Tidslinje, Periode, NAY, Kelvin, Arena.
---

# AAP domain for behandlingsflyt

Legal basis: folketrygdloven kapittel 11 (AAP); references in code are "§ 11-x". Kelvin = NAV's new AAP case system
(this app is its case-processing core); Arena = the legacy system cases are migrated from (`MIGRERING_FRA_ARENA`).

## 1. Glossary

| Term | Meaning (code anchor) |
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

## 2. Model essentials

- `Sak` 1 — n `Behandling`. Ytelsesbehandlinger form a singly linked list via `forrigeBehandlingId` (a revurdering points to
  the behandling it builds on; a fasttrack revurdering may be inserted in the middle). Klage/SvarFraAndreinstans/
  OppfølgingsBehandling have `forrigeBehandlingId = null`.
- `TypeBehandling` vs *faktisk* type: a revurdering after a behandling without rett is treated as a førstegangsbehandling
  for kvalitetssikring, beslutter and statistikk (`BehandlingService.utledFaktiskBehandlingstype`).
- `Sak.rettighetsperiode` is shared by all behandlinger and can change (earlier søknad, overstyring, trukket søknad sets an
  invalid/fictive period). It is not stored per behandling.
- Behandling status: OPPRETTET → UTREDES → IVERKSETTES → AVSLUTTET, derived from the active step. `erAvsluttet()` includes IVERKSETTES.

## 3. Roles (`no.nav.aap.tilgang.Rolle`, enforced via aap-tilgang)

SAKSBEHANDLER_OPPFOLGING (Nav-kontor: sykdom, bistand, arbeidsevne, overgang uføre/arbeid, refusjon…) → KVALITETSSIKRER →
SAKSBEHANDLER_NASJONAL (NAY: yrkesskade, student, sykepengeerstatning, beregning, medlemskap, samordning, barnetillegg,
foreslå vedtak…) → BESLUTTER (fatte vedtak, vedtaksbrev). `Definisjon.løsesAv` lists the roles per behov; API endpoints
use `authorizedGet/Post` / `getGrunnlag` with `Operasjon` (SE, SAKSBEHANDLE, DRIFTE, DRIFT_LES) and a `relevanteIdenterResolver`.

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

*`obligatorisk` = `StartBehandlingSteg` creates IKKE_VURDERT periods for it in a førstegangsbehandling (technical, not legal).
Vilkår results are periodised (`Tidslinje<Vilkårsvurdering>`); `Vilkårsvurderer.vurder` must be pure.
`TidligereVurderinger` combines results of steps before a given step to decide "uunngåelig avslag" / "ingen behandlingsgrunnlag"
so later steps can skip work.

## 5. Rettighetstype (`RettighetsType`)

BISTANDSBEHOV (§ 11-6, ordinær kvote), SYKEPENGEERSTATNING (§ 11-13), STUDENT (§ 11-14, ordinær kvote), ARBEIDSSØKER
(§ 11-17), VURDERES_FOR_UFØRETRYGD (§ 11-18). Derived per period in `FASTSETT_RETTIGHETSTYPE` from vilkårsresultatet via the
priority list in `KravForRettighetsType.kt`; worked examples (gantt) in `behandlingsflyt/behandlingsflyt.md`.

## 6. From vurderinger to money (Revurdering flow order)

krav/rettighetsperiode → lovvalg → meldeperioder → alder → sykdom-gruppe (Nav-kontor) → kvalitetssikring → yrkesskade/student/
sykepengeerstatning → sykdomsvilkåret → beregning (grunnlag) → inntektsbortfall → medlemskap → oppholdskrav → barnetillegg →
institusjon → samordning → effektuer 11-7 → **rettighetstype** → vedtakslengde → meldeplikt → **underveis** →
**tilkjent ytelse** → simulering → foreslå vedtak → **fatte vedtak** (vedtak lagres) → **iverksett** (job
`flyt.iverksettUtbetaling` → aap-utbetal; varsler; datadeling) → **brev** (vedtaksbrev via aap-brev).
Exact list: `python3 .agents/skills/flyt-orkestrering/scripts/flyt.py --flyt Revurdering`.

## 7. External systems (gateways registered in `repository/.../integrasjon/DefaultGatewayProvider.kt`)

PDL (person, identer, barn), POPP/inntekt + A-inntekt (inntektskomponenten), AAREG/EREG (arbeidsforhold), MEDL (medlemskap),
Inst2 (institusjonsopphold), yrkesskade-registeret, Pesys/uføre, TP (tjenestepensjon), Abakus (sykepenger, foreldrepenger),
dagpenger, tiltakspenger, aap-arenaoppslag (Arena), aap-utbetal (utbetaling, simulering), aap-brev + pdfgenerator (brev),
aap-oppgave (oppgavestyring), aap-statistikk, aap-api-intern/SAM (datadeling, samordning id), aap-meldekort-backend,
dokumentinnhenting (legeerklæring), dokarkiv, Gosys, NOM/NORG (enheter), Kabal, aap-tilgang (PIP), Unleash.
Inbound Kafka (`api/.../hendelse/kafka`): Kabal, PDL hendelser, tilbakekreving, sykepengevedtak, institusjonsopphold,
uførevedtak, foreldrepengevedtak. Inbound documents: `POST /api/hendelse/send` from postmottak.
Outbound events: `flyt.hendelse` job → aap-oppgave (`BehandlingFlytStoppetHendelse`), statistikk, datadeling.

## 8. Tidslinje / Periode (komponenter `tidslinje`, `verdityper`)

- `Periode(fom, tom)`: whole days, **both ends inclusive**; `fom > tom` throws. `Tid.MIN` = 0001-01-01, `Tid.MAKS` = 2999-01-01.
- `Tidslinje<T>`: sorted non-overlapping `Segment(periode, verdi)`; constructing with overlaps **throws**. Gaps are allowed.
- `Tidslinje.map2/map3/…` and `outerJoin` are **outer joins**: the lambda gets nulls where one side has no value and the
  result covers the union. Use `innerJoin`/`leftJoin`/`kryss` when you mean intersection/left side.
- `map`/`mapValue` keep segment boundaries; call `komprimer()` to merge equal neighbours. `begrensetTil(periode)` clips;
  `komplement(periode) { … }` fills gaps; `filter`, `segment(dato)`, `helePerioden()`, `erSammenhengende()`.
- Build with `tidslinjeOf(periode to verdi, …)` or `liste.somTidslinje { it.periode }`; empty: `Tidslinje.empty()` / `.orEmpty()`.
- Test helper: `assertTidslinje` in `lib-test/.../help/AssertTidslinje.kt`.
