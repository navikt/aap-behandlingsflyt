# Dokumentkilder i individuell begrunnelse

Avklaringsbehov `5053` (`SKRIV_SYKDOMSVURDERING_BREV`) støtter en frivillig, ordnet
liste med dokumentkilder. Begrunnelsen og kildevalgene lagres separat og versjoneres
sammen. Endring i kildevalg, rekkefølge, dokumentnavn eller dato regnes som endring av
vurderingen. Kilder kopieres ikke automatisk til en ny revurdering.

## Innsending og lesing

Bruk eksisterende innsending av avklaringsbehov med følgende felter:

```json
{
  "behovstype": "5053",
  "vurdering": "Den individuelle begrunnelsen.",
  "kilder": [
    {
      "journalpostId": "123",
      "dokumentInfoId": "456",
      "dokumentnavn": "Legeerklæring",
      "journalføringsdato": "2026-01-20"
    }
  ]
}
```

`journalpostId` og `dokumentInfoId` identifiserer til sammen et dokument. Flere
dokumenter fra samme journalpost kan velges, men samme dokument kan ikke velges
flere ganger. Referansene og dokumentnavnet kan ikke være blanke. Journalføringsdato
er påkrevd og skal være faktisk journalføringsdato, ikke mottaksdato.

Manglende eller `null` kildefelt bevarer tidligere valg og betyr tom liste ved første
lagring. `kilder: []` fjerner alle valg. En oppgitt liste erstatter tidligere valg i
den nye vurderingsversjonen. Kilder erstatter ikke kravet til individuell begrunnelse.

`GET /api/behandling/{referanse}/grunnlag/sykdomsvurdering-for-brev` returnerer samme
kildefelter på `vurdering.kilder` og på hver oppføring i `historiskeVurderinger`.
Listen er tom for eldre vurderinger uten kildevalg. Navn og dato er øyeblikksbilder
fra vurderingen og oppdateres ikke automatisk fra dokumentlisten.

Eksisterende tilgangskontroll og låsing etter kvalitetssikring gjelder fortsatt.
Backend validerer format og duplikater, men gjør ikke nye dokumentoppslag for å
verifisere klientinnsendte metadata eller eierskap. Integrasjonen skal hente valgene
fra eksisterende tilgangsstyrte saksdokumentliste; den listen er utenfor denne endringen.

## Brevtekst og forhåndsvisning

Ved brevutledning legges kildelisten til etter begrunnelsen i eksisterende
`Faktagrunnlag.Sykdomsvurdering`. Ingen ny brevkontrakt er innført.

```text
Den individuelle begrunnelsen.

Vi har lagt vekt på følgende dokumenter i vurderingen:
- Annen dokumentasjon (24.01.2026)
- Legeerklæring (20.01.2026)
```

Formatteringen bruker `dd.MM.yyyy`, parenteser, én kilde per linje og valgorden.
Uten kilder er begrunnelsen uendret; det legges ikke til en tom liste eller
introduksjon. Ved gjentatt utledning genereres teksten alltid fra rå begrunnelse og
kildefelter, ikke fra et tidligere formattert brev.

## Kobling i frontend og brevtjenesten

Frontendendringene må gjøres i frontendprosjektet:

- Bruk eksisterende saksdokumentliste til «Legg til», med dokumentidentitet som nøkkel.
- Vis og fjern valg under «Dokumenter brukt i vurderingen». Kilder er frivillige.
- Vis introduksjon og ordnet kildeliste under «Slik vil kildene vises i brevet» fra
  gjeldende skjemaverdier. Bruk eksemplet ovenfor som formatteringskontrakt.
- Ta med hele kildevalget ved bekreftelse og i eksisterende mellomlagrede skjemadata.
  Ikke legg generert kildetekst inn i `vurdering`.
- Gjenopprett kildevalg ved gjenåpning og vis historiske øyeblikksbilder. Respekter
  `kanSaksbehandle`; manglende metadata må vises som feil, ikke erstattes med en annen dato.

Begge relevante brevbyggere mottar samme tekst via henholdsvis v2- og v3-bestilling.
Faktisk HTML/PDF må kontrolleres i brevtjenesten for at linjeskift og listen er
lesbare, dokumentnavn behandles som tekst, og kildene vises sammen med begrunnelsen
for beslutter. Denne backendendringen endrer ikke renderer eller frontend og
omskriver ikke allerede bestilte eller ferdigstilte brev.
