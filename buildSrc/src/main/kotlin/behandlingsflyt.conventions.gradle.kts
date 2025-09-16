// Felles kode for alle build.gradle.kts filer som laster inn denne conventions pluginen

plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = "no.nav.aap"
version = project.findProperty("version")?.toString() ?: "0.0.0"

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    mavenLocal()
}

testing {
    suites {
        @Suppress("UnstableApiUsage") val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}


tasks {
    test {
        useJUnitPlatform()
        maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    (tasks.findByName("distTar") as? Tar)?.apply {
        // Bruk et unikt navn for jar-filen til distTar, for å unngå navnekollisjoner i multi-modul prosjekt,
        // slik at vi ikke bruker samme navn, feks. "kontrakt.jar" "api.jar" i flere moduler.
        // Dette unngår feil av typen "Entry <name>.jar is a duplicate but no duplicate handling strategy has been set"
        archiveBaseName.set("${rootProject.name}-${project.name}")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
    }
}

// Pass på at når vi kaller JavaExec eller Test tasks så bruker vi samme JVM som vi kompilerer med
val toolchainLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
}
tasks.withType<Test>().configureEach { javaLauncher.set(toolchainLauncher) }
tasks.withType<JavaExec>().configureEach { javaLauncher.set(toolchainLauncher) }


kotlin.sourceSets["main"].kotlin.srcDirs("main")
kotlin.sourceSets["test"].kotlin.srcDirs("test")
sourceSets["main"].resources.srcDirs("main")
sourceSets["test"].resources.srcDirs("test")