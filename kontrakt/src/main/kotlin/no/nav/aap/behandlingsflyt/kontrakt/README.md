# Kontrakt

Kontraktmodulen tilgjengeliggjøres som bibliotek:

```
implementation("no.nav.aap.behandlingsflyt:kontrakt:$behandlingsflytVersjon")
```

Vær dermed ekstra oppmerksom på bakoverkompatibilitet, og at brukere av biblioteket må bumpe versjon ved endringer.
F.eks. dersom man legger til en `Definisjon.kt`, er man nødt til å bumpe versjonen
i [tilgang](https://github.com/navikt/aap-tilgang), [oppgave](https://github.com/navikt/aap-oppgave)
og [statistikk](https://github.com/navikt/aap-statistikk) for at disse appene skal vite om det nye behovet.