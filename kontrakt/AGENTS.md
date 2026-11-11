# kontrakt — supplements the root AGENTS.md (read that too)

This module is published as `no.nav.aap.behandlingsflyt:kontrakt` and used by other Kelvin apps (aap-tilgang, aap-oppgave,
aap-statistikk, postmottak, frontend). See `src/main/kotlin/no/nav/aap/behandlingsflyt/kontrakt/README.md`.

- Changes must be backwards compatible. Never rename or remove enum constants (`StegType`, `StegGruppe`, `Definisjon`,
  `AvklaringsbehovKode`, statuses, `TypeBehandling`, statistikk `Vurderingsbehov`/`Vilkårtype`): names are persisted in
  databases and sent as JSON. Deprecate instead.
- Avklaringsbehov codes are unique forever — never reuse a code (`Definisjon` checks uniqueness at class init).
- New `Definisjon` values or other enum values: consumers must bump the kontrakt version to understand them; mention this
  to the user.
- Explicit API mode is on: keep `public` modifiers. Jackson annotations define the wire format.
- Run `./gradlew :kontrakt:check` after changes; regenerate `.agents/skills/avklaringsbehov/references/definisjoner.md`
  (`python3 .agents/skills/avklaringsbehov/scripts/avklaringsbehov.py --markdown > …`) when `Definisjon` changes.
