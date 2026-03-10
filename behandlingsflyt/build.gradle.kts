
plugins {
    id("aap.conventions")
}

tasks{
    val projectProps by registering(WriteProperties::class) {
        destinationFile = layout.buildDirectory.file("behandlingsflyt-version.properties")
        // Define property.
        property("project.version", getCheckedOutGitCommitHash())
    }

    processResources {
        // Depend on output of the task to create properties,
        // so the properties file will be part of the Java resources.

        from(projectProps)
    }
}

dokka {
    dokkaSourceSets.configureEach {
        includes.from("behandlingsflyt.md")
    }
}

fun getCheckedOutGitCommitHash(): String {
    if (System.getenv("GITHUB_ACTIONS") == "true") {
        return System.getenv("GITHUB_SHA")
    }
    return runCommand("git rev-parse --verify HEAD")
}

fun runCommand(command: String): String {
    val execResult = providers.exec {
        this.workingDir = project.projectDir
        commandLine(command.split("\\s".toRegex()))
    }.standardOutput.asText

    return execResult.get()
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
    api(libs.oppgaveKontrakt)
    api(libs.meldekortKontrakt)
    api(libs.motor)
    api(libs.gateway)
    api(libs.utbetalKontrakt)
    implementation(libs.dbconnect)
    // TODO: fjern denne avhengigheten når alle RestClient-instanser er i repository-modulen
    implementation(libs.httpklient)
    implementation(libs.json)
    implementation(libs.infrastructure)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)
    implementation(libs.kafkaClients)
    implementation(kotlin("reflect"))
    implementation(libs.flywayDatabasePostgresql)
    runtimeOnly(libs.postgresql) // låst versjon i root build.gradle.kts


    testImplementation(project(":lib-test"))
    testImplementation(project(":repository"))
    testImplementation(libs.dbtest)
    testImplementation(libs.tabletest)
    implementation(libs.motorTestUtils)
    testImplementation(libs.bundles.junit)

    testImplementation(libs.testcontainersPostgres)
    constraints {
        implementation("org.apache.commons:commons-compress:1.28.0") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation(libs.mockk)
    testImplementation(kotlin("test"))
}
