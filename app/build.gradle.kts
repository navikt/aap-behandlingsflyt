import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.processResources
import java.io.ByteArrayOutputStream

val ktorVersion = "3.0.0"

plugins {
    id("behandlingsflyt.conventions")
    id("io.ktor.plugin") version "3.0.0"
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

fun runCommand(command: String): String {
    val byteOut = ByteArrayOutputStream()
    project.exec {
        commandLine = command.split("\\s".toRegex())
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim()
}

fun getCheckedOutGitCommitHash(): String {
    if (System.getenv("GITHUB_ACTIONS") == "true") {
        return System.getenv("GITHUB_SHA")
    }
    return runCommand("git rev-parse --verify HEAD")
}


val komponenterVersjon = "1.0.39"

dependencies {
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")


    implementation("io.micrometer:micrometer-registry-prometheus:1.13.5")
    implementation("ch.qos.logback:logback-classic:1.5.11")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    implementation("no.nav.aap.brev:kontrakt:0.0.8")
    implementation("no.nav.aap.statistikk:api-kontrakt:0.0.25")
    implementation("no.nav.aap.kelvin:motor:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")

    implementation(project(":integrasjon"))
    implementation(project(":sakogbehandling"))
    implementation(project(":faktagrunnlag"))
    implementation(project(":verdityper"))
    implementation(project(":tidslinje"))
    implementation(project(":dbflyway"))
    implementation(project(":pip"))
    implementation("com.zaxxer:HikariCP:6.0.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.18.2")
    runtimeOnly("org.postgresql:postgresql:42.7.4")

    testImplementation(project(":dbtestdata"))
    testImplementation(project(":lib-test"))
    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testImplementation("no.nav.aap.kelvin:motor-test-utils:$komponenterVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.testcontainers:postgresql:1.20.3")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation(kotlin("test"))
}
