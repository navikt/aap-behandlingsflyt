---
name: nytt-grunnlag
description: Oppskrift for ny grunnlagstype med migrasjon, domenemodell, Repository-grensesnitt, Postgres-impl, in-memory-impl, registreringer og tester. Bruk når noe nytt skal lagres per behandling.
---
# Nytt grunnlag med repository

Les `.github/instructions/repository.instructions.md` og `.github/instructions/migrasjoner.instructions.md`. Reelt
eksempel: `f84ecaaa3` (sykepenger/ferie fra søknaden, 9 filer). Kopier mønsteret fra `SykdomRepositoryImpl` +
`SykdomRepositoryImplTest`.

1. Domene: `faktagrunnlag/<opphav>/<feature>/XxxGrunnlag.kt`, `XxxVurdering.kt` (data classes med `vurdertIBehandling`,
   `opprettet`, `Bruker`).
2. Grensesnitt `XxxRepository : Repository` (fra `no.nav.aap.lookup.repository`) i samme pakke:
   `lagre(behandlingId, …)`, `hentHvisEksisterer(behandlingId)`, evt. `deaktiverGrunnlag`.
3. Migrasjon `repository/src/main/resources/flyway/V1.<n>__xxx.sql`: `xxx_grunnlag(id, behandling_id, aktiv, <peker>_id, opprettet_tid)`
   + `xxx_vurdering(...)` (+ `xxx_vurderinger` bindetabell ved 1:n). Indeks på `(behandling_id) WHERE aktiv`.
4. `repository/.../faktagrunnlag/<opphav>/<feature>/XxxRepositoryImpl.kt`: hent (aktiv), lagre (deaktiver+insert hvis
   ulikt), `kopier` (INSERT … SELECT av aktiv rad), `slett` (hard, idempotent), `companion object : Factory<…>`.
5. Registrer i `repository/.../PostgresRepositoryRegistry.kt`.
6. `lib-test/.../inmemoryrepo/InMemoryXxxRepository.kt` med samme `aktiv`-semantikk; registrer i
   `InMemoryRepositoryRegistry.kt`.
7. Tester: `XxxRepositoryImplTest` mot `TestDataSource()` (lagre/hent/deaktiver/kopier/slett).
8. Skal dataene vises: `api/.../XxxGrunnlagApi.kt` + `App.kt`.
