import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm") version "1.9.23"
    id("io.ktor.plugin") version "2.3.10" apply false
}

group = "no.nav.aap"

val javaVersion = 21

allprojects {
    repositories {
        mavenCentral()
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

tasks.withType(Test::class) {
    testLogging {
        events = setOf(TestLogEvent.FAILED,
                 TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks {
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
