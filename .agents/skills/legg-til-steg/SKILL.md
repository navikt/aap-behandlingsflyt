c---
name: legg-til-steg
description: Step-by-step recipes (with file paths and code skeletons) for common changes in behandlingsflyt — a new or changed steg in a flow, a new vurdering/avklaringsbehov (Definisjon + løsning + løser + grunnlag/repository + API), a new informasjonskrav (register data), a new vilkår, a new Vurderingsbehov, a new feature toggle, a new gateway/integration. Use when implementing features that touch the flow, so nothing in the wiring is forgotten. Keywords — nytt steg, ny vurdering, nytt avklaringsbehov, ny løsning, ny løser, informasjonskrav, vilkår, vurderingsbehov, feature toggle, gateway, integrasjon, grunnlag-API.
---

# Recipes for changing the flow

Read first if unfamiliar: flyt-orkestrering (engine), avklaringsbehov (behov lifecycle). Skeletons:
[references/maler.md](references/maler.md). Find exemplars quickly with `flyt.py --steg <StegType>` and
`avklaringsbehov.py --definisjon <NAVN>`. Newer features are grouped as vertical packages (`no.nav.aap.bistandsbehov`,
`no.nav.aap.meldeplikt`, `no.nav.aap.overgangarbeid`: steg, vilkår, vurdering, grunnlag, repository interface together;
impl in `repository/` and API in `api/` under the same package) — prefer that for new features.

## A. New step (steg) in a flow

1. `kontrakt/.../steg/StegType.kt`: add `NY_STEG(gruppe = StegGruppe.X, status = Status.UTREDES)`. Pick the `StegGruppe` the UI
   should show it under (new group → frontend change). Status IVERKSETTES only for post-vedtak steps. Never rename/remove existing values.
2. Step class (`forretningsflyt/steg/` or the feature package): `class NyttSteg(deps) : BehandlingSteg` + secondary
   constructor `(RepositoryProvider, GatewayProvider)` + `companion object : FlytSteg { konstruer(); type() = StegType.NY_STEG }`.
   Optional `override val rekkefølge` in the companion to order several behov within the step.
3. `utfør` must be idempotent and decide relevance itself: `when (kontekst.vurderingType)` (exhaustive),
   `tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())`, `kontekst.vurderingsbehovRelevanteForSteg`.
   Reconcile behov via `AvklaringsbehovService` (never hand-create). Side effects to other systems → enqueue a motor job.
4. `forretningsflyt/behandlingstyper/Revurdering.kt` (also used by Førstegangsbehandling) or the relevant flow: insert
   `.medSteg(steg = NyttSteg, vurderingsbehovRelevanteForSteg = listOf(...), informasjonskrav = listOf(...))` at the right
   position (before `.sluttÅOppdatereFaktagrunnlag()` unless it is a post-vedtak step). The vurderingsbehov list = which
   behov the step should see AND which ones rewind the flow to it.
5. If the step can conclude avslag/no grunnlag for later steps: add a `Sjekk(StegType.NY_STEG)` in `TidligereVurderingerImpl`
   **in flow order** (it `require`s ordering at construction).
6. Frontend/API: grunnlag endpoint (§B.5). `StegGruppe` with `skalVises = true` appears in the UI.
7. Verify: `python3 .agents/skills/flyt-orkestrering/scripts/flyt.py --flyt Revurdering` then regenerate references
   (`flyt.py --markdown > .agents/skills/flyt-orkestrering/references/flyter.md`), unit-test the step with fakes, and run
   the closest flyt-test (testing skill). Never all flyt-tests in one Gradle run; if the change is broad, use
   `kjor_test.py --batch flyt` and run the batches one per tool call. Consider open behandlinger
   already past the insertion point (flyt-orkestrering §11).

## B. New vurdering = avklaringsbehov + løsning + løser + grunnlag + API

1. **kontrakt** `avklaringsbehov/AvklaringsbehovKode.kt`: `public const val NY_KODE: String = "50xx"` + enum value; `Definisjon.kt`:
   `NY_VURDERING(kode = AvklaringsbehovKode.\`50xx\`, type = BehovType.MANUELT_PÅKREVD, løsesISteg = StegType.X,
   kreverToTrinn = …, kvalitetssikres = …, løsesAv = listOf(Rolle.…))`. Never reuse codes. Tell the user that consumers
   (aap-tilgang, aap-oppgave, aap-statistikk, frontend) must bump the kontrakt version.
2. **Domain**: vurdering data class (+ `PeriodisertVurdering` if periodised: `fom`, `tom`, `vurdertIBehandling`, `vurdertAv`),
   grunnlag class, repository interface extending `no.nav.aap.lookup.repository.Repository`.
3. **Persistence** (repository-og-migrering skill): Flyway migration (`repo.py neste-migrering --navn …`), `XRepositoryImpl` with
   `companion object : Factory<XRepositoryImpl>`, `kopier` + `slett`, register in `PostgresRepositoryRegistry.kt`, and an
   `InMemory…` variant in lib-test if unit tests need it. Repository test in `repository/src/test`.
4. **Løsning + løser**: `behandling/avklaringsbehov/løsning/NyLøsning.kt` (`@JsonTypeName(value = NY_KODE)`,
   `EnkeltAvklaringsbehovLøsning` or `PeriodisertAvklaringsbehovLøsning<Dto>`; sealed → this package only) delegating to
   `løser/NyLøser.kt` (`AvklaringsbehovsLøser<NyLøsning>`): validate (throw `UgyldigForespørselException` for user errors),
   merge with `forrigeBehandlingId`'s vurderinger when periodised, save, return `LøsningsResultat(begrunnelse)`.
   Jackson subtypes are discovered automatically (`utledSubtypesTilAvklaringsbehovLøsning`).
5. **Step**: in the step with `type() == løsesISteg`, call `avklaringsbehovService.oppdaterAvklaringsbehov(...)` or the
   periodised variant every run; implement `tilbakestillGrunnlag` (reset to forrige behandling's vurderinger).
6. **Vilkår** (if the vurdering decides a vilkår): `Vilkårsvurderer<F : Faktagrunnlag>` object with `vilkårtype` and a pure
   `vurder(faktagrunnlag): Tidslinje<Vilkårsvurdering>` (feature toggles go INTO the faktagrunnlag, never read inside
   `vurder`); call `VilkårService(repositoryProvider).vurderVilkår(behandlingId, faktagrunnlag, Vilkår)` from the step. New
   `Vilkårtype` entries need a kontrakt statistikk `Vilkårtype` too.
7. **API**: `api/.../XGrunnlagApi.kt` with `getGrunnlag<BehandlingReferanse, XGrunnlagResponse>(…, påkrevdRolle =
   Definisjon.NY_VURDERING.løsesAv)` in a `readOnly` transaction; register the route in `app/.../App.kt` (`routing { … apiRouting { … } }`).
   Solving uses the generic `POST /api/behandling/løs-behov` — no new endpoint.
8. **Tests** (testing skill): løser unit test with in-memory repos; repository test; flow test with a `løsNyVurdering()`
   helper in `AbstraktFlytOrkestratorTest`; API test if the response is non-trivial.
9. Regenerate `.agents/skills/avklaringsbehov/references/definisjoner.md`.

## C. New informasjonskrav (fetch/refresh external facts)

1. `InformasjonskravNavn` new value (persisted in `informasjonskrav_oppdatert`; never rename).
2. Class implementing `Informasjonskrav<Input, Registerdata>` + `companion object : Informasjonskravkonstruktør`
   (`navn`, `konstruer`). Respect the phase rules: `klargjør` (DB, no HTTP) → `hentData` (HTTP, **no DB**, runs in parallel)
   → `oppdater` (DB, compare with stored grunnlag, return ENDRET only on real change). `erRelevant`: limit to
   relevant vurderingTyper, skip when `TidligereVurderinger` gives avslag, throttle with `oppdatert.ikkeKjørtSisteKalenderdag()`
   or similar — it runs on every pass through `forbered` otherwise.
3. Gateway interface + impl (§F) for the external source.
4. Register on the step that needs the data: `informasjonskrav = listOf(NyttInformasjonskrav)` in the flow. ENDRET for a
   step before the active one rewinds the flow to that step.
5. If an atomic (meldekort/fasttrack) revurdering may produce data the open behandling needs, override
   `flettOpplysningerFraAtomærBehandling`.

## D. New Vurderingsbehov

1. kontrakt `statistikk/StoppetBehandling.kt` → `enum class Vurderingsbehov` (public; used by other apps/frontend).
2. `sakogbehandling/flyt/Vurderingsbehov.kt` internal enum + `tilVurderingsbehov()`; `prosessering/statistikk/mappere.kt`
   (`tilKontraktVurderingsbehov`); `periodisering/FlytKontekstMedPeriodeService.vurderingsbehovTilType`. The compiler flags
   the exhaustive `when`s.
3. Add it to `vurderingsbehovRelevanteForSteg` for each step that must react (check `flyt.py --vurderingsbehov`), and to the
   steps' `tvingerAvklaringsbehov` if it should force a behov. Remember `Vurderingsbehov.alle()` excludes only G_REGULERING.
4. Fasttrack candidate? Add to `BehandlingService.fasttrackKandidater` only if the whole flow can complete without humans.
5. If created from documents: `hendelse/mottak/MottattHendelseUtleder.kt`.

## E. Feature toggle

1. Add to `BehandlingsflytFeature` (`unleash/FeatureToggle.kt`); create it in Unleash (aap-unleash-web). `default` is used
   when Unleash is unavailable.
2. **Add it to `LokalUnleash` in `lib-test/.../FakeUnleash.kt`** (it throws "feature toggle X ikke definert for fake").
   `AlleAvskruddUnleash` needs nothing (everything off except `IngenValidering`).
3. Use `unleashGateway.isEnabled(BehandlingsflytFeature.X)` (gateway from `gatewayProvider.provide()`); for per-sak rollout
   `erPåskruddForSak(...)`. Never read toggles inside `Vilkårsvurderer.vurder`.

## F. New gateway (HTTP integration)

1. Interface in `behandlingsflyt` extending `no.nav.aap.komponenter.gateway.Gateway`.
2. Impl in `repository/src/main/kotlin/.../integrasjon/<area>/` with `companion object : Factory<Interface>`; config via
   `requiredConfigForKey("INTEGRASJON_X_URL")` / `_SCOPE`; `RestClient.withDefaultResponseHandler(config, AzureM2MTokenProvider, prometheus)`.
3. Register in `defaultGatewayProvider()` (`repository/.../integrasjon/DefaultGatewayProvider.kt`) **and** `testGatewayProvider()`
   (`lib-test/.../TestGatewayProvider.kt`).
4. Env vars in `.nais/dev.yaml` and `.nais/prod.yaml` (+ accessPolicy outbound). Fake server in `lib-test/.../fakes/`,
   started and wired with `System.setProperty("INTEGRASJON_X_URL", …)` in `FakeServers.kt`.
5. Call it only from `Informasjonskrav.hentData`, a motor job, or a dedicated service — not inside DB-heavy loops.
