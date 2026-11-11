# Flytmotor (`flyt/`) — tillegg til AGENTS.md i rota (les den også)

Last inn skillen `flyt-orkestrering` før du endrer noe her. Alle flyter og alle åpne behandlinger i produksjon kjører på
denne tilstandsmaskinen.

Invarianter som koden bygger på:
- Faser i et steg: START → OPPDATER_FAKTAGRUNNLAG → UTFØRER → AVKLARINGSPUNKT → AVSLUTTER (`StegStatus.rekkefølge()`; KDoc-en til
  `StegOrkestrator` utelater OPPDATER_FAKTAGRUNNLAG). AVKLARINGSPUNKT er eneste stoppunkt. Tilbakeføring skriver bare TILBAKEFØRT.
- `StegOrkestrator` oppretter DB-savepoints etter START og OPPDATER_FAKTAGRUNNLAG (med mindre `markSavepointAt` er overstyrt).
  Ved feil ruller dbconnect tilbake til siste savepoint og committer resten.
- `BehandlingFlyt` er en tilstandshåndterende peker (`aktivtSteg`). `behandling.flyt()` returnerer en ny peker på det første steget.
  `Tilbakeflyt`-instanser flytter forelderens peker bakover.
- Tilbakeføringer (`tilbakefør`) kjører ingen stegkode og hoppes over for behandlinger med status IVERKSETTES/AVSLUTTET.
- `prosesserBehandling` publiserer `behandlingHendelseService.stoppet(...)` ved hvert stopp og på slutten, men ikke når den avsluttes
  på grunn av `stoppNårStatus` (atomiske kjøringer).

Tester: `BehandlingFlytTest`, `EnklereFlytOrkestratorTest`, `FlytOrkestratorTest` og flyttester i `src/test/.../flyt/`.
