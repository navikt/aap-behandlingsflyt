# aap-behandlingsflyt

Behandlingsflyt for Arbeidsavklaringspenger (AAP). Definerer flyten for ulike behandlingstyper, og styrer prosessen med
å drive saksflyten fremover.

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