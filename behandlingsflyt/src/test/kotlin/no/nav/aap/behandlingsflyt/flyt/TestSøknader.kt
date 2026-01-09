package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KommeTilbake
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManueltOppgittBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppgitteBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UtenlandsPeriodeDto
import java.time.LocalDate

object TestSøknader {
    val STANDARD_SØKNAD = `SøknadV0`(
        student = `SøknadStudentDto`(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
        medlemskap = `SøknadMedlemskapDto`("JA", "JA", "NEI", "NEI", null)
    )

    val SØKNAD_INGEN_MEDLEMSKAP = `SøknadV0`(
        student = `SøknadStudentDto`(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
        medlemskap = `SøknadMedlemskapDto`(
            "JA", null, "JA", null,
            listOf(
                UtenlandsPeriodeDto(
                    "SWE",
                    LocalDate.now().plusMonths(1),
                    LocalDate.now().minusMonths(1),
                    "JA",
                    null,
                    LocalDate.now().plusMonths(1),
                    LocalDate.now().minusMonths(1),
                )
            )
        )
    )

    val SØKNAD_STUDENT = `SøknadV0`(
        student = `SøknadStudentDto`(StudentStatus.Ja, KommeTilbake.Ja),
        yrkesskade = "NEI",
        oppgitteBarn = null,
        medlemskap = `SøknadMedlemskapDto`("JA", "NEI", "NEI", "NEI", null)
    )

    val SØKNAD_MED_BARN: (List<Pair<String, LocalDate>>) -> `SøknadV0` = { barn ->
        `SøknadV0`(
            student = null,
            yrkesskade = "NEI",
            oppgitteBarn = OppgitteBarn(
                identer = emptySet(),
                barn = barn.map { (navn, fødseldato) ->
                    ManueltOppgittBarn(
                        navn = navn,
                        fødselsdato = fødseldato,
                        ident = null,
                        relasjon = ManueltOppgittBarn.Relasjon.FOSTERFORELDER
                    )
                }
            ),
            medlemskap = `SøknadMedlemskapDto`("JA", "NEI", "NEI", "NEI", null)
        )
    }

    val SØKNAD_YRKESSKADE = STANDARD_SØKNAD.copy(yrkesskade = "JA")
}