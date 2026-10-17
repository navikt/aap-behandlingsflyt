plugins {
    id("aap.conventions")
}

// Arkitekturtester: leser kildekoden til de andre modulene med Konsist, uten å kompilere dem.
// Kjøres med ./gradlew :arkitektur:test (ca. 10 s, uten Docker).
dependencies {
    testImplementation(libs.konsist)
    testImplementation(kelvinLibs.bundles.junit)
    testImplementation(kotlin("test"))
}

tasks.test {
    // Testene leser filer Gradle ellers ikke vet om. Uten dette regnes testen som oppdatert (eller hentes fra
    // byggecachen) selv om koden i en annen modul er endret, og regelbrudd slipper gjennom.
    inputs.files(
        fileTree(rootDir) {
            include("*/src/main/**/*.kt", "repository/src/main/resources/flyway/*.sql")
            exclude("arkitektur/**")
        }
    ).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("kildekode")
}
