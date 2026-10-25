---
name: verifiser
description: Kjør riktig sett med kompilering og tester for filene som er endret, og rapporter resultatet ærlig. Bruk før du sier at noe er ferdig, og alltid før en PR. Tar valgfritt en basis-ref (for eksempel origin/main) for en ren arbeidskopi.
argument-hint: "[basis-ref, for eksempel origin/main]"
---
# Verifiser endringene

1. Finn endrede filer. Er arbeidskopien skitten (`git status --porcelain` gir noe), bruk
   `git status --porcelain | awk '{print $2}'`. Er den ren, bruk `git diff --name-only <basis>...HEAD`, der basis
   er det brukeren skrev etter `/verifiser` (for eksempel `/verifiser origin/main`).
   Finnes ingen basis, si det og stopp. Moduler er første stikomponent: `| cut -d/ -f1 | sort -u`.
2. Kompiler hver endret modul: `./gradlew :<modul>:compileKotlin :<modul>:compileTestKotlin`.
3. Kjør kjernetestene for hver endret modul: `./gradlew :<modul>:coreTest` (uten Docker, 10-20 s).
   Feiler den fordi en test prøver å bruke Docker, mangler testklassen `@Tag("db")`/`@Tag("kafka")`: legg den til.
   Kjør alltid `./gradlew :arkitektur:test` (strukturregler, ~10 s) i et eget kall; `coreTest` og `test` kan ikke
   stå i samme Gradle-kall. Feiler den, rett koden etter meldingen; ikke legg nye brudd inn i `kjenteBrudd`. Er et
   kjent brudd rettet, fjern det fra listen.
4. Kjør DB- og flyttester for hver endret fil i listen fra steg 1, mest spesifikt først:
   - Endret `XxxSteg.kt` → `./gradlew :behandlingsflyt:test --tests "*XxxStegTest"` og deretter
     `--tests "*FlytOrkestratorTest" --tests "*FlytTest"` (flytsekvens og scenarier).
   - Endret løser/løsning → `--tests "*XxxLøserTest" --tests "*AvklaringsbehovsLøserTest"`.
   - Endret informasjonskrav → `--tests "*XxxInformasjonskravTest" --tests "*InformasjonskravGrunnlagTest"`.
   - Endret `*RepositoryImpl` eller migrasjon → `./gradlew :repository:test --tests "*XxxRepositoryImplTest"`,
     deretter hele `:repository:test` (alle migrasjoner kjøres mot tom Postgres).
   - Endret `kontrakt/` → `./gradlew :kontrakt:test` og si eksplisitt at kontrakten endres.
   - Endret `api/` → `./gradlew :api:test --tests "*XxxApiTest"`; endret `App.kt` → `./gradlew :app:test`.
5. Docker må kjøre for alt som er tagget `db`/`kafka`/`flyt`. Hvis Testcontainers feiler med Ryuk/Docker, er det
   miljøet, ikke koden – si det, ikke "fiks" testen.
6. Rapporter: hvilke kommandoer som kjørte, antall tester, hva som feilet med feilmelding. Ikke si "alle tester
   grønne" uten å ha kjørt dem. Ikke endre forventninger i tester for å få dem grønne uten å forklare hvorfor.
