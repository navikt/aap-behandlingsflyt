package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapDataIntern
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import java.time.LocalDate

object TestPersoner {
    val STANDARD_PERSON = {
        FakePersoner.leggTil(
            TestPerson(
                fødselsdato = `Fødselsdato`(LocalDate.now().minusYears(20)),
                yrkesskade = emptyList(),
                sykepenger = emptyList()
            )
        )
    }

    val PERSON_MED_YRKESSKADE = {
        FakePersoner.leggTil(
            TestPerson(
                fødselsdato = `Fødselsdato`(LocalDate.now().minusYears(25)),
                yrkesskade = listOf(TestYrkesskade(skadedato = LocalDate.now().minusYears(1))),
            )
        )
    }

    val PERSON_FOR_UNG = {
        FakePersoner.leggTil(
            TestPerson(
                fødselsdato = `Fødselsdato`(LocalDate.now().minusYears(17))
            )
        )
    }

    val PERSON_62 = {
        FakePersoner.leggTil(
            TestPerson(
                fødselsdato = `Fødselsdato`(LocalDate.now().minusYears(62))
            )
        )
    }

    val PERSON_61 = {
        FakePersoner.leggTil(
            TestPerson(
                fødselsdato = `Fødselsdato`(LocalDate.now().minusYears(61))
            )
        )
    }

    val PERSON_MED_FORUTGÅENDE_MEDLEMSKAP = {
        FakePersoner.leggTil(
            TestPerson(
                fødselsdato = `Fødselsdato`(LocalDate.now().minusYears(20)),
                yrkesskade = emptyList(),
                sykepenger = emptyList(),
                medlStatus = listOf(
                    MedlemskapDataIntern(
                        unntakId = 100087727,
                        ident = "",
                        fraOgMed = LocalDate.now().minusYears(20).toString(),
                        tilOgMed = LocalDate.now().toString(),
                        status = "GYLD",
                        statusaarsak = null,
                        medlem = true,
                        grunnlag = "grunnlag",
                        lovvalg = "lovvalg",
                        helsedel = true,
                        lovvalgsland = "NOR",
                        kilde = null
                    )
                )
            )
        )
    }
}