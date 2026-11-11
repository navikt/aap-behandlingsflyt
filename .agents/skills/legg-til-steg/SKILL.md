c---
name: legg-til-steg
description: Oppskrifter trinn for trinn, med filstier og kodeskjeletter, for vanlige endringer i behandlingsflyt: et nytt eller endret steg i en flyt, en ny vurdering/et nytt avklaringsbehov (Definisjon + løsning + løser + grunnlag/repository + API), et nytt informasjonskrav (registerdata), et nytt vilkår, et nytt Vurderingsbehov, en ny feature toggle eller en ny gateway/integrasjon. Bruk når du lager funksjonalitet som berører flyten, så du ikke glemmer noe i koblingene. Nøkkelord — nytt steg, ny vurdering, nytt avklaringsbehov, ny løsning, ny løser, informasjonskrav, vilkår, vurderingsbehov, feature toggle, gateway, integrasjon, grunnlag-API.
---

# Oppskrifter for å endre flyten

Les først hvis du ikke kjenner temaet: flyt-orkestrering (motor), avklaringsbehov (livssyklus for behov). Kodeskjeletter:
[references/maler.md](references/maler.md). Finn raskt eksempler med `flyt.py --steg <StegType>` og
`avklaringsbehov.py --definisjon <NAVN>`. Nyere funksjonalitet er samlet i vertikale pakker (`no.nav.aap.bistandsbehov`,
`no.nav.aap.meldeplikt`, `no.nav.aap.overgangarbeid`: steg, vilkår, vurdering, grunnlag og repository-grensesnitt samlet;
implementasjon i `repository/` og API i `api/` under samme pakke). Bruk helst denne strukturen for ny funksjonalitet.

## A. Nytt steg i en flyt

1. `kontrakt/.../steg/StegType.kt`: legg til `NY_STEG(gruppe = StegGruppe.X, status = Status.UTREDES)`. Velg `StegGruppe` som UI-et
   skal vise steget under (ny gruppe → endring i frontend). Bruk statusen IVERKSETTES bare for steg etter vedtak. Ikke gi eksisterende verdier nye navn eller fjern dem.
2. Stegklasse (`forretningsflyt/steg/` eller feature-pakken): `class NyttSteg(deps) : BehandlingSteg` + sekundær
   konstruktør `(RepositoryProvider, GatewayProvider)` + `companion object : FlytSteg { konstruer(); type() = StegType.NY_STEG }`.
    Legg eventuelt til `override val rekkefølge` i companion for å sortere flere behov i steget.
3. `utfør` må være idempotent og selv avgjøre om steget er relevant: `when (kontekst.vurderingType)` (uttømmende),
   `tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())`, `kontekst.vurderingsbehovRelevanteForSteg`.
    Avstem behov via `AvklaringsbehovService` (ikke opprett dem manuelt). Sideeffekter i andre systemer → legg en motor-jobb i kø.
4. `forretningsflyt/behandlingstyper/Revurdering.kt` (brukes også av Førstegangsbehandling) eller den relevante flyten: sett inn
   `.medSteg(steg = NyttSteg, vurderingsbehovRelevanteForSteg = listOf(...), informasjonskrav = listOf(...))` på riktig
   riktig sted (før `.sluttÅOppdatereFaktagrunnlag()` hvis det ikke er et steg etter vedtak). Listen over vurderingsbehov angir hvilke
    behov steget skal se OG hvilke som fører til tilbakeføring av flyten til steget.
5. Hvis steget kan konkludere med avslag/ikke grunnlag for senere steg: legg til en `Sjekk(StegType.NY_STEG)` i `TidligereVurderingerImpl`
    **i flytrekkefølge** (`require` kontrollerer rekkefølgen under oppretting).
6. Frontend/API: grunnlag-endepunkt (§B.5). `StegGruppe` med `skalVises = true` vises i UI-et.
7. Kontroller: Kjør `python3 .agents/skills/flyt-orkestrering/scripts/flyt.py --flyt Revurdering`, og generer referansene på nytt
    (`flyt.py --markdown > .agents/skills/flyt-orkestrering/references/flyter.md`). Skriv enhetstest for steget med fakes, og kjør
    den nærmeste flyttesten (testing-skillen). Kjør aldri alle flyttestene i én Gradle-kjøring. Hvis endringen er omfattende, bruk
    `kjor_test.py --batch flyt` og kjør hver gruppe i et eget verktøykall. Ta hensyn til åpne behandlinger
    som allerede er forbi innsettingspunktet (flyt-orkestrering §11).

## B. Ny vurdering = avklaringsbehov + løsning + løser + grunnlag + API

1. **kontrakt** `avklaringsbehov/AvklaringsbehovKode.kt`: `public const val NY_KODE: String = "50xx"` + enum-verdi; `Definisjon.kt`:
   `NY_VURDERING(kode = AvklaringsbehovKode.\`50xx\`, type = BehovType.MANUELT_PÅKREVD, løsesISteg = StegType.X,
    kreverToTrinn = …, kvalitetssikres = …, løsesAv = listOf(Rolle.…))`. Ikke bruk koder på nytt. Fortell brukeren at konsumentene
    (aap-tilgang, aap-oppgave, aap-statistikk, frontend) må oppgradere kontrakt-versjonen.
2. **Domene**: vurdering-data class (+ `PeriodisertVurdering` hvis periodisert: `fom`, `tom`, `vurdertIBehandling`, `vurdertAv`),
    grunnlag-class, repository-grensesnitt som utvider `no.nav.aap.lookup.repository.Repository`.
3. **Lagring** (repository-og-migrering-skillen): Flyway-migrering (`repo.py neste-migrering --navn …`), `XRepositoryImpl` med
    `companion object : Factory<XRepositoryImpl>`, `kopier` + `slett`, registrering i `PostgresRepositoryRegistry.kt` og en
    `InMemory…`-variant i lib-test hvis enhetstestene trenger den. Repository-test i `repository/src/test`.
4. **Løsning + løser**: `behandling/avklaringsbehov/løsning/NyLøsning.kt` (`@JsonTypeName(value = NY_KODE)`,
   `EnkeltAvklaringsbehovLøsning` eller `PeriodisertAvklaringsbehovLøsning<Dto>`; sealed → bare denne pakken) som delegerer til
    `løser/NyLøser.kt` (`AvklaringsbehovsLøser<NyLøsning>`): valider (kast `UgyldigForespørselException` ved brukerfeil),
    slå sammen vurderinger fra `forrigeBehandlingId` hvis de er periodisert, lagre og returner `LøsningsResultat(begrunnelse)`.
    Jackson finner undertypene automatisk (`utledSubtypesTilAvklaringsbehovLøsning`).
5. **Steg**: I steget der `type() == løsesISteg`, kall `avklaringsbehovService.oppdaterAvklaringsbehov(...)` eller den
    periodiserte varianten ved hver kjøring. Implementer `tilbakestillGrunnlag` (tilbakestill til vurderingene fra forrige behandling).
6. **Vilkår** (hvis vurderingen avgjør et vilkår): `Vilkårsvurderer<F : Faktagrunnlag>`-objekt med `vilkårtype` og en ren
    `vurder(faktagrunnlag): Tidslinje<Vilkårsvurdering>`-funksjon (feature toggles legges INN i faktagrunnlaget, de leses aldri i
    `vurder`). Kall `VilkårService(repositoryProvider).vurderVilkår(behandlingId, faktagrunnlag, Vilkår)` fra steget. Nye
    `Vilkårtype`-verdier trenger også en `Vilkårtype` for kontrakt-statistikk.
7. **API**: `api/.../XGrunnlagApi.kt` med `getGrunnlag<BehandlingReferanse, XGrunnlagResponse>(…, påkrevdRolle =
    Definisjon.NY_VURDERING.løsesAv)` i en `readOnly`-transaksjon. Registrer ruta i `app/.../App.kt` (`routing { … apiRouting { … } }`).
    Løsning bruker det generiske `POST /api/behandling/løs-behov`. Ikke lag et nytt endepunkt.
8. **Tester** (testing-skillen): enhetstest av løseren med in-memory-repositories, repository-test, flyttest med en `løsNyVurdering()`-
    hjelpefunksjon i `AbstraktFlytOrkestratorTest`, og API-test hvis responsen ikke er triviell.
9. Generer `.agents/skills/avklaringsbehov/references/definisjoner.md` på nytt.

## C. Nytt informasjonskrav (hent/oppdater eksterne fakta)

1. Ny verdi i `InformasjonskravNavn` (lagres i `informasjonskrav_oppdatert`; ikke gi den nytt navn).
2. Klasse som implementerer `Informasjonskrav<Input, Registerdata>` + `companion object : Informasjonskravkonstruktør`
    (`navn`, `konstruer`). Følg fasereglene: `klargjør` (DB, ingen HTTP) → `hentData` (HTTP, **ingen DB**, kjører parallelt)
    → `oppdater` (DB, sammenlign med lagret grunnlag, returner ENDRET bare ved reell endring). `erRelevant`: begrens til
    relevante vurderingTyper, hopp over hvis `TidligereVurderinger` gir avslag, begrens kjøringene med `oppdatert.ikkeKjørtSisteKalenderdag()`
    eller tilsvarende. Ellers kjører kravet ved hver gjennomgang i `forbered`.
3. Gateway-grensesnitt + implementasjon (§F) for den eksterne kilden.
4. Registrer kravet på steget som trenger dataene: `informasjonskrav = listOf(NyttInformasjonskrav)` i flyten. ENDRET for et
    steg før det aktive steget fører til at flyten går tilbake til dette steget.
5. Hvis en atomisk (meldekort/fasttrack) revurdering kan produsere data som den åpne behandlingen trenger, overstyr
   `flettOpplysningerFraAtomærBehandling`.

## D. Nytt Vurderingsbehov

1. kontrakt `statistikk/StoppetBehandling.kt` → `enum class Vurderingsbehov` (public; brukes av andre apper/frontend).
2. `sakogbehandling/flyt/Vurderingsbehov.kt` intern enum + `tilVurderingsbehov()`; `prosessering/statistikk/mappere.kt`
   (`tilKontraktVurderingsbehov`); `periodisering/FlytKontekstMedPeriodeService.vurderingsbehovTilType`. Kompilatoren varsler om
   uttømmende `when`-uttrykk.
3. Legg det til i `vurderingsbehovRelevanteForSteg` for hvert steg som skal reagere (sjekk `flyt.py --vurderingsbehov`), og i
    stegenes `tvingerAvklaringsbehov` hvis det skal tvinge fram et behov. Husk at `Vurderingsbehov.alle()` bare utelater G_REGULERING.
4. Aktuelt for fasttrack? Legg det til i `BehandlingService.fasttrackKandidater` bare hvis hele flyten kan fullføres uten mennesker.
5. Hvis det opprettes fra dokumenter: `hendelse/mottak/MottattHendelseUtleder.kt`.

## E. Feature toggle

1. Legg det til i `BehandlingsflytFeature` (`unleash/FeatureToggle.kt`), og opprett det i Unleash (aap-unleash-web). `default` brukes
    når Unleash ikke er tilgjengelig.
2. **Legg det til i `LokalUnleash` i `lib-test/.../FakeUnleash.kt`** (den kaster «feature toggle X ikke definert for fake»).
    `AlleAvskruddUnleash` trenger ingen endring (alt er av, bortsett fra `IngenValidering`).
3. Bruk `unleashGateway.isEnabled(BehandlingsflytFeature.X)` (gateway fra `gatewayProvider.provide()`); for utrulling per sak,
    bruk `erPåskruddForSak(...)`. Les aldri toggles inne i `Vilkårsvurderer.vurder`.

## F. Ny gateway (HTTP-integrasjon)

1. Grensesnitt i `behandlingsflyt` som utvider `no.nav.aap.komponenter.gateway.Gateway`.
2. Implementasjon i `repository/src/main/kotlin/.../integrasjon/<area>/` med `companion object : Factory<Interface>`; konfigurer via
    `requiredConfigForKey("INTEGRASJON_X_URL")` / `_SCOPE`; `RestClient.withDefaultResponseHandler(config, AzureM2MTokenProvider, prometheus)`.
3. Registrer i `defaultGatewayProvider()` (`repository/.../integrasjon/DefaultGatewayProvider.kt`) **og** `testGatewayProvider()`
    (`lib-test/.../TestGatewayProvider.kt`).
4. Miljøvariabler i `.nais/dev.yaml` og `.nais/prod.yaml` (+ utgående accessPolicy). Fake-server i `lib-test/.../fakes/`,
    start og koble den til med `System.setProperty("INTEGRASJON_X_URL", …)` i `FakeServers.kt`.
5. Kall den bare fra `Informasjonskrav.hentData`, en motor-jobb eller en egen tjeneste, ikke i løkker med mye DB-arbeid.
