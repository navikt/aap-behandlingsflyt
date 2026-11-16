package no.nav.aap.behandlingsflyt.test

import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.ImageNameSubstitutor

class DockerIkkeTillattIKjernetester : ImageNameSubstitutor() {
    override fun apply(original: DockerImageName): DockerImageName =
        error(
            "En test med -PcoreTestsOnly=true prøver å bruke Docker (Testcontainers ville startet " +
                "${original.asCanonicalNameString()}). Kjernetester skal kjøre uten Docker: legg @Tag(\"db\") (Postgres) " +
                "eller @Tag(\"kafka\") på testklassen, så kjøres den av `test` i stedet."
        )

    override fun getDescription(): String = "Kjernetester kjører uten Docker"
}
