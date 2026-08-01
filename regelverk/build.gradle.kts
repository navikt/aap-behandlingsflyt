plugins {
    id("aap.conventions")
}

dokka {
    dokkaSourceSets.configureEach {
//        includes.from("behandlingsflyt.md")
    }
}

fun runCommand(command: String): String {
    val execResult = providers.exec {
        this.workingDir = project.projectDir
        commandLine(command.split("\\s".toRegex()))
    }.standardOutput.asText

    return execResult.get().trim()
}

dependencies {
    api(project(":kontrakt"))
    implementation(libs.micrometerRegistryPrometheus)
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.opentelemetryInstrumentationAnnotations)

    api(libs.tilgangPlugin)
    api(libs.tilgangKontrakt)
    api(libs.brevKontrakt)
    api(libs.meldekortKontrakt)
    api(libs.motor)
    api(libs.gateway)
    api(libs.utbetalKontrakt)
    api(libs.dokumentinnhentingKontrakt)
    implementation(libs.dbconnect)
    // TODO: fjern denne avhengigheten når alle RestClient-instanser er i repository-modulen
    implementation(libs.httpklient)
    implementation(libs.json)
    implementation(libs.infrastructure)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)
    implementation(libs.kafkaClients)
    implementation(kotlin("reflect"))


    testImplementation(project(":lib-test"))
    testImplementation(project(":repository"))
    testImplementation(libs.dbtest)
    testImplementation(libs.tabletest)
    implementation(libs.motorTestUtils)
    testImplementation(libs.bundles.junit)

    constraints {
        implementation("org.apache.commons:commons-compress:1.28.0") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation(libs.mockk)
    testImplementation(kotlin("test"))
}
