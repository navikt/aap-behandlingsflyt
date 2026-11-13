package no.nav.aap.behandlingsflyt.test

import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.ImageNameSubstitutor

/**
 * Sørger for at `coreTest` aldri starter Docker. Gradle-tasken setter `TESTCONTAINERS_IMAGE_SUBSTITUTOR` til denne
 * klassen, og Testcontainers kaller den før hver container startes. En test som trenger Docker, men mangler tag,
 * feiler da med en forklaring i stedet for å bruke 10-15 sekunder på å starte Postgres og migrere.
 */
class DockerIkkeTillattIKjernetester : ImageNameSubstitutor() {
    override fun apply(original: DockerImageName): DockerImageName =
        throw IllegalStateException(
            "En test i coreTest prøver å bruke Docker (Testcontainers ville startet " +
                "${original.asCanonicalNameString()}). coreTest skal kjøre uten Docker: legg @Tag(\"db\") (Postgres) " +
                "eller @Tag(\"kafka\") på testklassen, så kjøres den av `test` i stedet."
        )

    override fun getDescription(): String = "coreTest kjører uten Docker"
}
