# kontrakt — tillegg til AGENTS.md i rota (les den også)

Denne modulen publiseres som `no.nav.aap.behandlingsflyt:kontrakt` og brukes av andre Kelvin-apper (aap-tilgang, aap-oppgave,
aap-statistikk, postmottak og frontend). Se `src/main/kotlin/no/nav/aap/behandlingsflyt/kontrakt/README.md`.

- Endringer må være bakoverkompatible. Gi aldri enum-konstanter nytt navn eller fjern dem (`StegType`, `StegGruppe`, `Definisjon`,
  `AvklaringsbehovKode`, statuser, `TypeBehandling`, statistikkens `Vurderingsbehov`/`Vilkårtype`). Navnene lagres i
  databaser og sendes som JSON. Merk dem deprecated i stedet.
- Koder for avklaringsbehov er unike for alltid. Ikke bruk en kode på nytt (`Definisjon` kontrollerer at kodene er unike ved oppstart av klassen).
- Når du legger til verdier i `Definisjon` eller andre enum-er, må konsumentene oppgradere kontraktversjonen for å forstå dem. Informer
  brukeren om dette.
- Explicit API-modus er aktiv. Behold `public`-modifikatorer. Jackson-annotasjoner definerer wire-formatet.
- Kjør `./gradlew :kontrakt:check` etter endringer. Generer `.agents/skills/avklaringsbehov/references/definisjoner.md`
  på nytt (`python3 .agents/skills/avklaringsbehov/scripts/avklaringsbehov.py --markdown > …`) når `Definisjon` endres.
