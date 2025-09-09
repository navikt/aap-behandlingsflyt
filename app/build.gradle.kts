import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val opentelemetryVersion = "2.19.0-alpha"

plugins {
    id("behandlingsflyt.conventions")
    alias(libs.plugins.ktor)
}

application {
    mainClass.set("no.nav.aap.behandlingsflyt.AppKt")
}

tasks {
    val projectProps by registering(WriteProperties::class) {
        destinationFile = layout.buildDirectory.file("version.properties")
        // Define property.
        property("project.version", getCheckedOutGitCommitHash())
    }

    processResources {
        // Depend on output of the task to create properties,
        // so the properties file will be part of the Java resources.

        from(projectProps)
    }

    withType<ShadowJar> {
        mergeServiceFiles()
    }
}

tasks.register<JavaExec>("runTestApp") {
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("no.nav.aap.behandlingsflyt.TestAppKt")
}

tasks.register<JavaExec>("genererOpenApiJson") {
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("no.nav.aap.behandlingsflyt.GenererOpenApiJsonKt")
}


tasks.register<JavaExec>("beregnCSV") {
    classpath = sourceSets.test.get().runtimeClasspath
    standardInput = System.`in`
    mainClass.set("no.nav.aap.behandlingsflyt.BeregnMedCSVKt")
}

fun runCommand(command: String): String {
    val execResult = providers.exec {
        this.workingDir = project.projectDir
        commandLine(command.split("\\s".toRegex()))
    }.standardOutput.asText

    return execResult.get()
}

fun getCheckedOutGitCommitHash(): String {
    if (System.getenv("GITHUB_ACTIONS") == "true") {
        return System.getenv("GITHUB_SHA")
    }
    return runCommand("git rev-parse --verify HEAD")
}

dependencies {
    implementation(libs.ktorServerCors)
    implementation(libs.ktorServerStatusPages)

    implementation("io.micrometer:micrometer-registry-prometheus:1.15.4")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

    implementation(libs.motor)
    implementation(libs.dbconnect)
    implementation(libs.dbmigrering)
    implementation(libs.motorApi)
    implementation(libs.json)
    implementation(libs.infrastructure)
    implementation(libs.server)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)

    // Auditlogging
    runtimeOnly(group = "com.papertrailapp", name = "logback-syslog4j", version = "1.0.0")

    implementation(project(":api"))
    implementation(project(":behandlingsflyt"))
    implementation(project(":repository"))
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.flywaydb:flyway-database-postgresql:11.12.0")
    runtimeOnly("org.postgresql:postgresql:42.7.7")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0:${opentelemetryVersion}")
    implementation("io.opentelemetry.instrumentation:opentelemetry-ktor-3.0:${opentelemetryVersion}")

    testImplementation(project(":lib-test"))
    implementation(libs.dbtest)
    implementation(libs.motorTestUtils)
    testImplementation(libs.bundles.junit)
    testImplementation("org.testcontainers:postgresql:1.21.3")
    constraints {
        implementation("org.apache.commons:commons-compress:1.28.0") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation(kotlin("test"))
    testImplementation("org.testcontainers:kafka:1.21.3")
    testImplementation("org.apache.kafka:kafka-clients:4.1.0")
}
