# Flyttester — tillegg til AGENTS.md i rota (les den også)

Last inn skillen `testing`. DSL, snapshot-tester og fallgruver er beskrevet i `.agents/skills/testing/references/flyttester.md`.

- Klasser som utvider `AbstraktFlytOrkestratorTest`/`AbstraktFlytOrkestratorSnapshotTest`, starter Postgres, alle fakes og
  motoren. Én klasse tar fra noen sekunder til over ett minutt. Kjør én klasse (maksimalt rundt tre per Gradle-kjøring), aldri
  `--tests '*FlytTest'`: `python3 .agents/skills/testing/scripts/kjor_test.py <Klasse> [-m "del av navn"]`.
  Ikke alle klassene i denne pakken er flyttester. `kjor_test.py --liste flyt` viser hvilke som er det.
- Kjed sammen kallene med `Behandling`-objektet hver DSL-hjelpemetode returnerer. Legg til en `løsX()`-hjelpemetode i
  `AbstraktFlytOrkestratorTest` for nye avklaringsbehov (`kjor_test.py --dsl` viser de eksisterende).
- Hvis en flyttest feiler på en merkelig måte, er det vanligvis en skjult jobbfeil: `kjor_test.py --resultat behandlingsflyt`.
- Utvid helst en eksisterende klasse. Bruk snapshot-basisklassen for mange tester som starter fra samme ferdige
  førstegangsbehandling.
