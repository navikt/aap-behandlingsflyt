import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.ByteArrayOutputStream

val ktorVersion = "2.3.7"

plugins {
    kotlin("jvm") version "1.9.21"
    id("io.ktor.plugin") version "2.3.7" apply false
}

val javaVersion = 21
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

subprojects {
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
        withType<ShadowJar> {
            mergeServiceFiles()
        }

        withType<Test> {
            reports.html.required.set(false)
            useJUnitPlatform()
            maxParallelForks = Runtime.getRuntime().availableProcessors()
        }
    }

    kotlin.sourceSets["main"].kotlin.srcDirs("main/kotlin")
    kotlin.sourceSets["test"].kotlin.srcDirs("test/kotlin")
    sourceSets["main"].resources.srcDirs("main/resources")
    sourceSets["test"].resources.srcDirs("test/resources")
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
