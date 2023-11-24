import java.io.ByteArrayOutputStream

val ktorVersion = "2.3.5"

plugins {
    kotlin("jvm") version "1.9.20"
    id("io.ktor.plugin") version "2.3.5"
}

application {
    mainClass.set("no.nav.aap.behandlingsflyt.AppKt")
}

val javaVersion = 21
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks {
        val projectProps by registering(WriteProperties::class) {
            destinationFile = file("${layout.buildDirectory}/version.properties")
            // Define property.
            property("project.version", getCheckedOutGitCommitHash())
        }

        processResources {
            // Depend on output of the task to create properties,
            // so the properties file will be part of the Java resources.
            from(projectProps)
        }
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.jvmTarget = "$javaVersion"
        }

        withType<Test> {
            reports.html.required.set(false)
            useJUnitPlatform()
            if (System.getenv("GITHUB_ACTIONS") != "true") {
                maxParallelForks = Runtime.getRuntime().availableProcessors()
            }
        }
    }

    kotlin.sourceSets["main"].kotlin.srcDirs("main")
    kotlin.sourceSets["test"].kotlin.srcDirs("test")
    sourceSets["main"].resources.srcDirs("main")
    sourceSets["test"].resources.srcDirs("test")
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
dependencies {
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("dev.forst:ktor-openapi-generator:0.6.1")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    implementation("io.micrometer:micrometer-registry-prometheus:1.11.4")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.flywaydb:flyway-core:9.22.3")
    runtimeOnly("org.postgresql:postgresql:42.6.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.testcontainers:postgresql:1.19.1")
    testImplementation(kotlin("test"))
}
