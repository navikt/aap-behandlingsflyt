package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.io.File

interface TestPersonService {
    fun leggTil(person: TestPerson): TestPerson

    fun oppdater(person: TestPerson): TestPerson

    fun hentPerson(ident: String): TestPerson?

    fun nullstillPersoner()

}

class JSONTestPersonService : TestPersonService {
    override fun leggTil(person: TestPerson): TestPerson {
        skrivJSON(person)
        return person
    }

    override fun oppdater(person: TestPerson): TestPerson {
        skrivJSON(person)
        return person
    }

    override fun hentPerson(ident: String): TestPerson? {
        return hentJSONFil().find { it.identer.first().identifikator == ident }
    }

    override fun nullstillPersoner() {
        val jsonFile = File("testpersoner.json")
        if (!jsonFile.exists()) {
            throw IllegalStateException("Could not find testpersoner.json")
        }
        jsonFile.writeText("[]")
    }

    private fun hentJSONFil(): List<TestPerson> {
        val jsonFile = File("testpersoner.json")
        if (!jsonFile.exists()) {
            throw IllegalStateException("Could not find testpersoner.json")
        }
        val jsonContent = jsonFile.readText()
        return DefaultJsonMapper.fromJson(jsonContent)
    }

    private fun skrivJSON(testPerson: TestPerson) {
        val jsonFile = File("testpersoner.json")
        if (!jsonFile.exists()) {
            // Create if not exists
            jsonFile.createNewFile()
        }

        val hentJSON = hentJSONFil()
        val nyJSON =
            if (hentJSON.any { it.identer.first().identifikator == testPerson.identer.first().identifikator }) {
                hentJSON.map { if (it.identer.first().identifikator == testPerson.identer.first().identifikator) testPerson else it }
            } else {
                hentJSON + testPerson
            }

        val jsonContent = DefaultJsonMapper.toJson(nyJSON)
        jsonFile.writeText(jsonContent)
    }
}