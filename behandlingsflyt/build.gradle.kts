
plugins {
    id("behandlingsflyt.conventions")
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
    implementation("io.micrometer:micrometer-registry-prometheus:1.16.0")
    implementation("ch.qos.logback:logback-classic:1.5.20")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.21.0")

    api(libs.tilgangPlugin)
    api(libs.tilgangKontrakt)
    api("no.nav.aap.brev:kontrakt:0.0.184")
    api("no.nav.aap.meldekort:kontrakt:0.0.147")
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
    implementation(kotlin("reflect"))
    implementation("org.flywaydb:flyway-database-postgresql:11.16.0")
    runtimeOnly("org.postgresql:postgresql") // låst versjon i root build.gradle.kts


    testImplementation(project(":lib-test"))
    testImplementation(project(":repository"))
    implementation(libs.dbtest)
    implementation(libs.motorTestUtils)
    testImplementation(libs.bundles.junit)

    testImplementation("org.testcontainers:postgresql:1.21.3")
    constraints {
        implementation("org.apache.commons:commons-compress:1.28.0") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation("io.mockk:mockk:1.14.6")
    testImplementation(kotlin("test"))
}
