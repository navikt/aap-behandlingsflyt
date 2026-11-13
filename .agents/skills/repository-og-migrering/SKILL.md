---
name: repository-og-migrering
description: Lagring i aap-behandlingsflyt: repository-grensesnitt mot implementasjoner og registre, mønsteret for versjonering av grunnlag/aktiv med delte, uforanderlige child-rader, semantikken for kopier (revurdering) og slett (GDPR), og hvilke repositories som IKKE kopierer, Flyway-migreringer (navngiving, neste nummer), bruk av dbconnect, radlåser og behandling-versjon, tidssoneregler. Bruk når du legger til eller endrer tabeller, repositories eller SQL, når data «forsvinner» eller «lekker» mellom behandlinger, eller når du skriver repository-tester. Nøkkelord — repository, RepositoryImpl, grunnlag, aktiv, kopier, slett, flyway, migrering, dbconnect, DBConnection, transaction, lås, versjon.
---

# Repositories, datamodell og migreringer

Verktøy: `python3 .agents/skills/repository-og-migrering/scripts/repo.py`
- `neste-migrering [--navn beskrivelse]` → neste Flyway-filnavn
- `oversikt [--filter X]` → hvert repository-grensesnitt → implementasjon, registrering (PG/MEM), implementasjon av `kopier`/`slett` | ingen handling | ikke relevant
- `sjekk XRepository` → kontroller koblingene for ett repository

## 1. Lag og koblinger

| Hva | Hvor |
|---|---|
| Grensesnitt (domene) | `behandlingsflyt/src/main/kotlin/…` (eller en feature-pakke), utvider `no.nav.aap.lookup.repository.Repository` (legger til `kopier` + `slett`) eller vanlige `no.nav.aap.komponenter.repository.Repository` (ingen kopiering/sletting: data på saksnivå, logger, jobber) |
| Implementasjon (SQL) | `repository/src/main/kotlin/…` (samme pakke som domeneområdet eller `…repository.<area>`), `class XRepositoryImpl(private val connection: DBConnection) : XRepository` + `companion object : Factory<XRepositoryImpl>` (`no.nav.aap.lookup.repository.Factory`). Noen eldre implementasjoner ligger i `behandlingsflyt` (`MottattDokumentRepositoryImpl`, `GjenopptakRepositoryImpl`) |
| Produksjonsregister | `repository/.../PostgresRepositoryRegistry.kt` → `.register<XRepositoryImpl>()` (registrert for hvert markørgrensesnitt den implementerer) |
| Testregister | `lib-test/.../inmemoryrepo/InMemoryRepositoryRegistry.kt` (+ `InMemoryXRepository`) for enhetstester uten DB |
| Bruk | `repositoryProvider.provide<XRepository>()` — en `RepositoryProvider` er knyttet til ÉN tilkobling/transaksjon og mellomlagrer instanser. Ikke behold repositories på tvers av transaksjoner. |

`GrunnlagKopiererImpl` og GDPR-slettingen i `SøknadSteg`/`TrekkKlageSteg` itererer over `repositoryProvider.provideAlle()` og kaller
`kopier`/`slett` på alle `no.nav.aap.lookup.repository.Repository`. Implementasjonen din tas automatisk med når den er registrert
(en implementasjon som er registrert under to markørgrensesnitt, kalles to ganger: sørg for at `kopier` er idempotent eller ikke gjør noe).

## 2. Grunnlagsmønsteret (versjonert per behandling, strukturelt delt)

```
X_GRUNNLAG(id, behandling_id, <child>_id, aktiv, opprettet_tid)     ← one ACTIVE row per behandling
X_VURDERINGER(id) / X_VURDERING(id, <set>_id, …)                    ← child rows, immutable once written
```
- `lagre(behandlingId, …)`: sett den aktive grunnlagsraden `aktiv = false`, sett inn nye child-rader, og sett inn en ny grunnlagsrad.
  Gamle rader blir stående som historikk.
- `hentHvisEksisterer(behandlingId)`: `WHERE behandling_id = ? AND aktiv`.
- `kopier(fra, til)` (kalles når en revurdering opprettes): sett inn en ny grunnlagsrad for `til` **som peker på de samme child-
  radene**. Flere behandlinger deler dermed child-radene. **Ikke bruk UPDATE eller DELETE på child-rader**; skriv nye rader
  og la grunnlagsraden peke på dem.
- `slett(behandlingId)`: slett personopplysninger for behandlingen permanent (idempotent). Kalles bare for en førstegangsbehandling
  der søknaden er trukket, eller en trukket klage. Implementasjoner kan regne med at ingen senere behandling deler radene.
- Periodiserte vurderinger: grunnlaget for en behandling inneholder alle gjeldende vurderinger, også de som er arvet fra
  tidligere behandlinger. Hver vurdering har `vurdertIBehandling`. Løsere lagrer vanligvis `forrige behandlings vurderinger + nye`
  (se `AvklarSykdomLøser`). «Ny i denne behandlingen» = `vurdertIBehandling == behandlingId`.

## 3. Hva som videreføres (og ikke) til en revurdering

`opprettRevurdering` kjører `kopier(forrige → ny)` for hvert repository. **Kopieres ikke** (`kopier` gjør ingenting eller er ikke relevant): avklaringsbehov,
vedtak, brevbestilling, behandling/steg_historikk, kontekst-logg (`ContextRepository`), informasjonskrav_oppdatert, PIP,
klagespesifikt grunnlag, avbrutt revurdering, uføre-søknad, sak/person, mellomlagring, Arena-migrering, SamId, … Kjør
`repo.py oversikt` for å se den gjeldende lista. (Dokka-sida `behandlingsflyt/behandlingsflyt.md` sier at vedtak, avklaringsbehov
og brev kopieres, men koden viser noe annet.) Klage, SvarFraAndreinstans og OppfølgingsBehandling starter med
`forrigeBehandlingId = null` og kopierer ingenting. Aktivitetsplikt kopierer fra forrige behandling av samme type.

## 4. Flyway

- Filer: `repository/src/main/resources/flyway/V1.<n>__<beskrivelse>.sql` (det finnes ett avvik, `V1.14.1__…`). Neste nummer:
  `repo.py neste-migrering`. Rekkefølgen er numerisk (V1.9 < V1.10).
- Kjøres ved oppstart av appen (`Migrering.migrate(fellesDataSource)` i `App.kt`) og av `TestDataSource()` i tester (classpath-
  plassering `flyway`). Ikke rediger en migrering som er merget. Legg til en ny. Skriv migreringer som fungerer mens den forrige app-
  versjonen fortsatt kjører (rolling deploy, flere pods): legg først til nullable kolonner eller standardverdier, og fjern dem senere.
- Konvensjoner: snake_case-navn; enums lagres med `name` (`setEnumName`/`getEnum`), så enum-konstanter må aldri få nytt
  navn; `behandling.type` lagrer `TypeBehandling.identifikator()` ("ae0034"), ikke enum-navnet; perioder som `daterange`
  (`setPeriode`/`getPeriode`); tidsstempler som `TIMESTAMP(3)` uten tidssone, skrevet som lokal tid i Oslo.
- Viktige tabeller: `sak`, `behandling` (`status`, `type`, `versjon`, `forrige_id`, `aarsak_til_opprettelse`), `steg_historikk`
  (én `aktiv`-rad per behandling), `avklaringsbehov` (unik per behandling+definisjon) + `avklaringsbehov_endring`,
  `vurderingsbehov` + `behandling_aarsak`, `mottatt_dokument`, `informasjonskrav_oppdatert`, `vilkar_resultat`/`vilkar`/`vilkar_periode`,
  `jobb` + `jobb_historikk` (+ `_arkiv`).

## 5. Grunnleggende om dbconnect (`no.nav.aap.komponenter.dbconnect`)

```kotlin
dataSource.transaction { connection -> … }                    // commit on success; readOnly = true for reads
connection.execute(sql) { setParams { setLong(1, id); setEnumName(2, status) }; setResultValidator { require(it == 1) } }
val id = connection.executeReturnKey(insertSql) { setParams { … } }
val n = connection.executeReturnUpdated(sql) { … }
connection.executeBatch(sql, items) { setParams { item -> setLong(1, item.id) } }
connection.queryFirstOrNull(sql) { setParams { … }; setRowMapper { row -> row.getLong("id") } }   // also queryFirst/List/Set
```
Kall `setParams` én gang per statement. Parametrene starter på indeks 1. Standard tidsavbrudd for spørringer er 30 s. `Row` har `get…`/`get…OrNull`,
`getEnum`, `getPeriode`, `getUUID`, `getArray`. Ved unntak ruller en transaksjon tilbake til siste `markerSavepoint()` og
committer resten (se motor-jobber §3). Ikke kall `markerSavepoint()` uten grunn.

## 6. Låser og versjon

`TaSkriveLåsRepository`: `lås(sakId, behandlingId)` / `låsSak` / `låsBehandling` / `withLås…` = `SELECT versjon … FOR UPDATE`
(alltid sak før behandling for å unngå deadlocks); `verifiserSkrivelås(lås)` = `UPDATE … SET versjon = versjon + 1 WHERE
versjon = ?` (må treffe én rad). Frontend sender `behandling.versjon` som `behandlingVersjon`. Hver prosesseringskjøring
øker versjonen, så en utdatert side får `OutdatedBehandlingException`. Jobber og API-skrivinger som endrer en behandling, tar disse låsene.

## 7. Tidssone

JVM-en og DB-sesjonen må bruke `Europe/Oslo` (`verifiserTidssone` feiler ved oppstart hvis ikke; Gradle-testoppgaven setter `TZ`).
Bruk `LocalDate`/`LocalDateTime` i Oslo-tid. `Tid.MAKS` = 2999-01-01 er «åpen slutt».

## 8. Repository-tester

`repository/src/test/...`: `TestDataSource()` i `@BeforeAll`, `dataSource.transaction { c -> val sak = sak(c);
val behandling = finnEllerOpprettBehandling(c, sak) … }` (hjelpemetoder i `lib-test/.../help/`), test `lagre`/`hent`/`kopier`/`slett`
inkludert at `kopier` + `lagre` på den nye behandlingen ikke endrer den gamle. Eksempel: `BistandRepositoryImplTest`.
Kjør bare den klassen (`python3 .agents/skills/testing/scripts/kjor_test.py BistandRepositoryImplTest`). Se testing-skillen for
testskjelett og regler for kjøring.
