# Tilgang
Hensikten til denne modulen er å tilby et grensesnitt for å kalle `tilgang`-tjenesten på en konsekvent måte.

## AuthourizedClient
Eksponerer extension-metoder for autoriserte get- og post-endepunkt. Disse kan anvendes i de andre modulene for å kalle `tilgang` automatisk etter autentisering. Dette gjøres ved at `TilgangPlugin` blir installert i routen.

## TilgangPlugin
Route scoped plugin for å intercepte http-kall og kalle `tilgang`. Attributter blir utledet fra path parameters eller request body, samt operasjonen som er definert ved bruk av metodene i `AuthorizedClient`. 

## TilgangGateway
Klienten som kaller `tilgang`-tjenesten. Denne brukes av `TilgangPlugin`, men kan også tas i bruk direkte i de tilfellene det er mer hensiktsmessig.

## TilgangInspections
Det er skrevet custom inspections for å gi advarsel dersom `get` og `post` brukes direkte. I de fleste tilfeller ønsker vi å bruke de tilsvarende <i>autoriserte</i> funksjonene, som vi finner i `AuthorizedClient`. Det anbefales å importere denne inspeksjonsfilen i IDE-en.    