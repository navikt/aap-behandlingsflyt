---
name: verifiser
description: Kjør riktig sett med kompilering og tester for filene som er endret, og rapporter resultatet ærlig. Bruk før du sier at noe er ferdig, og alltid før en PR. Tar valgfritt en basis-ref (for eksempel origin/main) for en ren arbeidskopi.
argument-hint: "[basis-ref, for eksempel origin/main]"
---
# Verifiser endringene

1. Finn endrede filer. Er arbeidskopien skitten (`git status --porcelain` gir noe), bruk
   `git status --porcelain | awk '{print $2}'`. Er den ren, bruk `git diff --name-only <basis>...HEAD`, der basis
   er ref-en brukeren oppga (for eksempel `origin/main`).
   Finnes ingen basis, si det og stopp. Moduler er første stikomponent: `| cut -d/ -f1 | sort -u`.
2. Kompiler hver endret modul: `./gradlew :<modul>:compileKotlin :<modul>:compileTestKotlin`.
3. Kjør testene uten Docker for hver endret modul: `./gradlew :<modul>:coreTest` (~10 s). De er raske, så kjør dem
   etter hver endring under arbeidet, ikke bare her. Feiler en test fordi den prøver å starte Docker
   (`DockerIkkeTillattIKjernetester`), mangler testklassen `@Tag("db")` eller `@Tag("kafka")`: legg den til.
4. Kjør DB- og flyttester for hver endret fil i listen fra steg 1, én testklasse per kall, mest spesifikt først.
   Flyttestene er tyngst: kjør bare den for området, aldri mange samtidig. Ikke kjør hele `:<modul>:test` eller
   `test`: det tar minutter, kan time ut og feiler ofte i en sandkasse. Det kjører CI.
   - Endret `XxxSteg.kt` → `./gradlew :behandlingsflyt:test --tests "*XxxStegTest"`, deretter flyttesten for området
     (for eksempel `--tests "*StudentFlytTest"`) og `--tests "*EnklereFlytOrkestratorTest"` (stegsekvensen).
   - Endret løser/løsning → `--tests "*XxxLøserTest" --tests "*AvklaringsbehovsLøserTest"`.
   - Endret informasjonskrav → `--tests "*XxxInformasjonskravTest" --tests "*InformasjonskravGrunnlagTest"`.
   - Endret `*RepositoryImpl` eller migrasjon → `./gradlew :repository:test --tests "*XxxRepositoryImplTest"`.
     Hver DB-testklasse kjører alle migrasjonene mot tom Postgres, så én klasse er nok til å sjekke en ny migrasjon.
   - Endret `kontrakt/` → `./gradlew :kontrakt:test` (få tester, uten Docker) og si eksplisitt at kontrakten endres.
   - Endret `api/` → `./gradlew :api:test --tests "*XxxApiTest"`; endret `App.kt` →
     `./gradlew :app:test --tests "no.nav.aap.behandlingsflyt.ApiTest"`.
5. Docker må kjøre for tester tagget `db`, `kafka` eller `flyt` (Testcontainers). Hvis Testcontainers feiler med
   Ryuk/Docker, er det miljøet, ikke koden – si det, ikke "fiks" testen.
6. Rapporter: hvilke kommandoer som kjørte, antall tester, hva som feilet med feilmelding. Ikke si "alle tester
   grønne" uten å ha kjørt dem. Ikke endre forventninger i tester for å få dem grønne uten å forklare hvorfor.
