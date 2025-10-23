# aap-behandlingsflyt

Behandlingsflyt for Arbeidsavklaringspenger (AAP). Definerer flyten for ulike behandlingstyper, og styrer prosessen med
å drive saksflyten fremover.

> **Note**
>
> Repoet kan inneholde regelverksendringer som ikke enda er vedtatt.
> Vi har derfor valgt å holde dette repoet lukket.

### API-dokumentasjon

APIene er dokumentert med Swagger: https://aap-behandlingsflyt.intern.dev.nav.no/swagger-ui/index.html

### Lokalt utviklingsmiljø:

AAP-Behandlingsflyt benytter test containers for integrasjonstester med databasen så et verktøy for å kjøre Docker
containers er nødvendig.<br>

For macOS og Linux anbefaler vi Colima. Det kan være nødvendig med et par tilpasninger:</br>

- `export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=$HOME/.colima/docker.sock`
- `export DOCKER_HOST=unix://$TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`
- `export TESTCONTAINERS_RYUK_DISABLED=true`

## Laste ned private pakker

For at Gradle skal finne private pakker på Github, legg dette i `~/.gradle/gradle.properties`

```
githubUser=<github-brukernavn>
githubPassword=<github-token>
```

Token må ha rettighet til å lese pakker. Husk å logg inn token med SSO for NAVIKT-organisasjonen.œ

## Kjøre lokalt

Appen har ulike run-konfigurasjoner i IntelliJ for å kjøre mot "fake" data eller mot dev-gcp.

Konfigurasjonsfilene finner du i mappen `.run/`

### TestApp (med fake data)

I IntelliJ skal det være plug-n-play med to ulike run-konfigurasjoner for TestApp.

**Uten TestContainers** (beholder data mellom app restarts)

Kjør `docker-compose up -d`

Deretter velg konfigurasjonen `TestApp` i Run/Debug-menyen.

**Med TestContainers** (databasen resettes ved hver app restart)

Velg konfigurasjonen `TestApp (med testcontainers)` i Run/Debug-menyen.

Alternativt, for å unngå å starte IntelliJ, gå i rotmappen og kjør:

```./gradlew runTestApp ```


Swagger-dokumentasjon blir tilgjengelig på http://localhost:8080/swagger-ui/index.html. For å gjøre autentiserte kall trengs JWT-token fra "fake Azure AD" (startet i Fakes-klassen). "Fake AD" kjører på localhost:8081, og du kan få token ved å kjøre

```
curl -s -XPOST http://localhost:8081/token  | jq -r '.access_token' | pbcopy 
```

Nå kan API-kall gjøres i Swagger UI ved å trykke på "Authorize"-knappen og lime inn token. For å gjøre API-kall i Swagger i dev kan man generere [maskin-til-maskin-token](http://azure-token-generator.intern.dev.nav.no/api/m2m?aud=dev-gcp:aap:behandlingsflyt)
eller [obo-token](http://azure-token-generator.intern.dev.nav.no/api/obo?aud=dev-gcp:aap:behandlingsflyt) vha. azure-token-generator.

### Mot dev-gcp

> ️⚠️ **_Bruk med omhu siden det potensielt kan ødelegge koblinger mellom oppgave og behandling i dev!_**
> 
> Bruk **TestApp** med mindre du har behov for dev-data fra eksterne systemer. 

Prosjektet inneholder en run config som kan kjøres av IntelliJ. Burde være synlig under "Run configurations" med navnet 
`dev-gcp.run.xml`.

For at det skal kjøre lokalt må du gjøre følgende:
1. Hent secret med [aap-cli/get-secret.sh](https://github.com/navikt/aap-cli): \
    `get-secret` \
2. Kjør opp lokal database med: \
    `docker-compose up -d`
3. Om du ønsker å hente data fra dev til lokal maskin kan du bruke [dump-gcp-db.sh](https://github.com/navikt/aap-cli?tab=readme-ov-file#dump-gcp-dbsh).
   Hvis du ikke henter data fra dev, får du beskjed om at rollen `cloudsqliamuser` mangler. Den kan denne legges inn ved å logge på databasen og kjøre følgende: \
    `CREATE ROLE cloudsqliamuser;`
4. Kjør `dev-gcp` fra IntelliJ.

Etter dette vil appen kjøre mot reelle data. Her kan du velge om du vil koble deg på gjennom autentisert frontend eller 
f.eks. gyldig token med cURL e.l.

OBS: Krever at du har `EnvFile`-plugin i IntelliJ. 