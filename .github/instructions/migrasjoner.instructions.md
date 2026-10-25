---
applyTo: "repository/src/main/resources/flyway/**"
description: "Flyway-migrasjoner: navngiving og rullende deploy (nullable eller DEFAULT, ingen DROP sammen med koden)"
---
# Regler for Flyway-migrasjoner

- Navn: `V1.<neste nummer>__beskrivelse_med_understrek.sql`. Sjekk høyeste eksisterende nummer; to PR-er med samme
  nummer kolliderer ved merge. Aldri rediger en migrasjon som er merget.
- Migrasjoner kjøres ved oppstart av **første nye pod** mens gamle poder (2–4 replicas, rullende deploy) fortsatt
  kjører gammel kode mot det nye skjemaet. Derfor:
  - Nye kolonner: nullable eller `NOT NULL DEFAULT …` (deretter evt. `DROP DEFAULT` senere).
  - `DROP COLUMN`/`DROP TABLE`/`RENAME`/`ALTER TYPE` bare i en egen PR etter at koden som brukte dem er i prod.
  - Ingen `CREATE INDEX CONCURRENTLY` i dag; store indekser låser under deploy – si fra.
- Datamigrasjoner (`UPDATE`/`DELETE`/PL/pgSQL) er sjeldne (7 %) og skal vurderes av et menneske. Foreslå heller en
  `Backfill*`-jobb i `app` hvis mengden er stor.
- Enum-navn (`StegType`, `Definisjon`, `Vurderingsbehov`, `InformasjonskravNavn`, jobbtyper) lagres som tekst i
  `steg_historikk`, `avklaringsbehov`, `vurderingsbehov`, `informasjonskrav`, `jobb`. En migrasjon som "rydder"
  i slike verdier må følges av `@Deprecated`-verdier i koden, ikke sletting.
- Det finnes ingen kompatibilitetstest for migrasjoner; alle `*RepositoryImplTest` kjører hele kjeden mot tom
  Postgres 18. Legg til/oppdater repository-testen i samme PR.
