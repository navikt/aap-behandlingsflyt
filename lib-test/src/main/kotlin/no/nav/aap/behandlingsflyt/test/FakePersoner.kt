package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.test.modell.TestPerson

object FakePersoner : TestPersonService {
    private val fakePersoner: MutableMap<String, TestPerson> = mutableMapOf()

    override fun leggTil(person: TestPerson): TestPerson {
        person.identer.forEach {
            fakePersoner[it.identifikator] = person
        }
        person.barn.forEach { leggTil(it) }
        return person
    }

    override fun oppdater(person: TestPerson): TestPerson {
        return leggTil(person)
    }

    override fun hentPerson(ident: String): TestPerson? {
        return fakePersoner[ident]
    }

    override fun nullstillPersoner() {
        fakePersoner.clear()
    }
}