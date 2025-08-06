package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import java.time.LocalDate

object FakePersoner {
    private val fakePersoner: MutableMap<String, TestPerson> = mutableMapOf()

    init {
        // testpersoner
        val BARNLØS_PERSON_10ÅR =
            TestPerson(
                identer = setOf(Ident("84837942045", true)),
                fødselsdato = Fødselsdato(
                    LocalDate.now().minusYears(10),
                )
            )
        val BARNLØS_PERSON_30ÅR =
            TestPerson(
                identer = setOf(Ident("12345678910", true)),
                fødselsdato = Fødselsdato(
                    LocalDate.now().minusYears(30),
                )
            )
        val BARNLØS_PERSON_18ÅR =
            TestPerson(
                identer = setOf(Ident("42346734567", true)),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(18).minusDays(10))
            )
        val PERSON_MED_BARN_65ÅR =
            TestPerson(
                identer = setOf(Ident("86322434234", true)),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(65)),
                barn = listOf(
                    BARNLØS_PERSON_18ÅR, BARNLØS_PERSON_30ÅR
                )
            )

        // Legg til alle testpersoner
        listOf(PERSON_MED_BARN_65ÅR, BARNLØS_PERSON_10ÅR).forEach { leggTil(it) }
    }

    fun leggTil(person: TestPerson): TestPerson {
        person.identer.forEach {
            if (fakePersoner[it.identifikator] != null) {
                throw IllegalStateException("Fakepersoner: Person med ident ${it.identifikator} finnes allerede fra før, så testen vil potensielt ha ugyldig tilstand")
            }
            fakePersoner[it.identifikator] = person
        }
        person.barn.forEach { leggTil(it) }
        return person
    }

    fun hentPerson(ident: String): TestPerson? {
        return fakePersoner[ident]
    }

    fun nullstillPersoner() {
        fakePersoner.clear()
    }
}