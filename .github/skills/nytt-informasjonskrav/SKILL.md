---
name: nytt-informasjonskrav
description: Oppskrift for nytt informasjonskrav / registeroppslag (gateway, fake, Informasjonskrav-klasse, InformasjonskravNavn, kravliste i flyten, toggle). Bruk når data skal hentes fra et register eller et nytt dokument skal inntas.
---
# Nytt informasjonskrav

Les `.github/instructions/informasjonskrav.instructions.md` og `.github/instructions/repository.instructions.md`.
Reelle eksempler: `a565fc176` (meldekortsyklus fra Arena, 8 filer) og `aead626f9` (tiltakspenger).
`a565fc176` kom uten tester; de kom senere i `33b0dc091`. Skriv testene i punkt 7 fra start.

1. Gateway: grensesnitt `XxxGateway : Gateway` i domenet; `XxxGatewayImpl` i `repository/.../integrasjon/<system>/`
   med `companion object : Factory<XxxGatewayImpl>`; registrer i `DefaultGatewayProvider.kt` **og**
   `lib-test/.../TestGatewayProvider.kt`; `Fake<System>` i `lib-test/.../fakes/` + `FakeServers`.
2. `faktagrunnlag/InformasjonskravNavn.kt`: ny verdi (lagres i DB; aldri omdøp).
3. `faktagrunnlag/<område>/XxxInformasjonskrav.kt`: `Informasjonskrav<Input, Registerdata>` med `erRelevant`
   (rate-limit!), `klargjør` (DB), `hentData` (eksternt, ingen DB), `oppdater` (sammenlign + lagre → ENDRET/IKKE_ENDRET),
   `companion object : Informasjonskravkonstruktør`. Implementer `KanTriggeRevurdering.behovForRevurdering` hvis
   endring i registeret skal kunne starte revurdering, og vurder om kravet skal inn i listen i
   `OppdagEndretInformasjonskravJobbUtfører`.
4. Repository for dataene (skillen `nytt-grunnlag`).
5. Flyt: `.medSteg(..., informasjonskrav = listOf(XxxInformasjonskrav))` på riktig(e) steg i `Revurdering.kt`.
   Dette endrer tilbakeføringsadferd.
6. Toggle: `BehandlingsflytFeature` + `lib-test/.../FakeUnleash.kt` (begge, alltid).
7. Tester: `XxxInformasjonskravTest` med `IKKE_ENDRET`/`ENDRET`-par og `erRelevant`; gateway-test mot faken.
