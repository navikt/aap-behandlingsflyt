---
applyTo: "kontrakt/**"
description: "kontrakt-modulen publiseres som bibliotek til andre apper: bare legg til, aldri endre eller fjerne"
---
# Regler for kontrakt-modulen

- Alt her publiseres automatisk som `no.nav.aap.behandlingsflyt:kontrakt` (ny patch-versjon per commit som rører
  `kontrakt/`) og konsumeres av aap-oppgave, aap-tilgang, aap-statistikk og aap-postmottak. En "opprydding" her er en
  ny publisert versjon. Stopp og be om bekreftelse før du endrer noe i denne modulen.
- Legg bare til; fjern aldri, omdøp aldri, endre aldri betydning. `@Deprecated` er verktøyet.
- Ny `Definisjon`/`StegType` krever at konsumentene bumper versjonen **før** verdien tas i bruk i flyten
  (`utviklerguide.md`), og at frontend-typene regenereres.
- Elleve enum-navn finnes både her og i domenet (`Vilkårtype`, `Utfall`, `Avslagsårsak`, `Vurderingsbehov`,
  `ÅrsakTilOpprettelse`, …). `Vilkårtype` og `Utfall` konverteres med `valueOf(name)` i runtime: ny verdi i
  domenet uten samme navn her feiler først i prod (statistikk-jobben).
- Hendelse-DTO-er (`SøknadV0`, `MeldekortV0`, …) lagres rått som JSON i `mottatt_dokument.strukturert_dokument`;
  gamle rader må fortsatt kunne deserialiseres. Ny form = ny `V<n+1>`-klasse.
- `explicitApi` står bare på warning-nivå; skriv likevel `public` eksplisitt.
