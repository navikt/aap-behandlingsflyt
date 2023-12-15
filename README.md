# aap-behandlingsflyt
Behandlingsflyt for Arbeidsavklaringspenger (AAP). Definerer flyten for ulike behandlingstyper, og styrer prosessen med 
å drive saksflyten fremover. 

### Lokalt utviklingsmiljø:
AAP-Behandlingsflyt benytter test containers for integrasjonstester med databasen så et verktøy for å kjøre Docker 
containers er nødvendig.<br>

For macOS og Linux anbefaler vi Colima. Det kan være nødvendig med et par tilpasninger:</br>
- `export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=$HOME/.colima/docker.sock`
- `export DOCKER_HOST=unix://$TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`
- `export TESTCONTAINERS_RYUK_DISABLED=true`

### API-dokumentasjon
APIene er dokumentert med Swagger: http://localhost:8080/swagger-ui/index.html