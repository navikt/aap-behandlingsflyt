import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import kotlin.math.max

// Felles kode for alle build.gradle.kts filer som laster inn denne conventions pluginen

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    `jvm-test-suite`
}

group = "no.nav.aap.behandlingsflyt"
version = project.findProperty("version")?.toString() ?: "0.0.0"

repositories {
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    mavenCentral()
    mavenLocal()
}

// https://docs.gradle.org/8.12.1/userguide/jvm_test_suite_plugin.html
testing {
    suites {
        @Suppress("UnstableApiUsage") val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}

dokka {
    dokkaSourceSets.configureEach {
        documentedVisibilities.set(VisibilityModifier.entries.toSet())
        sourceLink {
            remoteUrl("https://github.com/navikt/aap-behandlingsflyt/blob/main")
            localDirectory.set(rootDir)
        }
    }
    dokkaPublications {
        configureEach {
            suppressObviousFunctions.set(true)
            suppressInheritedMembers.set(false)
        }
    }
}

private fun bestemAntallTestTråder(): Int {
    val isCiBuild =
        providers.environmentVariable("CI").isPresent || providers.environmentVariable("GITHUB_ACTIONS").isPresent
    val processors = Runtime.getRuntime().availableProcessors()
    val antallTråder =
        if (isCiBuild) {
            (processors * 1.5).toInt() // vi har mye io-wait under testene våre
        } else {
            // reduser antall tråder ved lokal kjøring for å unngå at utvikler-maskinen blir for treg
            max(processors / 2, processors - 4)
        }

    return antallTråder
}

tasks {
    test {
        useJUnitPlatform()
        maxParallelForks = bestemAntallTestTråder()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    (findByName("jar") as? Jar)?.apply {
        // Bruk et unikt navn for jar-filen til hver submodul, for å unngå navnekollisjoner i multi-modul prosjekt,
        // gjennom at vi ikke bruker samme navn, feks. "kontrakt.jar" og "api.jar", i flere moduler.
        // Dette unngår feil av typen "Entry <name>.jar is a duplicate but no duplicate handling strategy has been set"
        // Alternativet er å unngå å bruke det eksakt samme navnet på submoduler fra forskjellige moduler,
        // som feks "kontrakt".
        archiveBaseName.set("${rootProject.name}-${project.name}")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)

        // Bruk et unikt navn for <submodule>.kotlin_module for hver Gradle-submodul, for å unngå navnekollisjoner i
        // multi-modul prosjekt, hvor vi inkluderer flere av våre kotlin-moduler i samme jar-fil eller
        // på samme runtime classpath. Kroneksempelet er "kontrakt.kotlin_module" fra både behandlingsflyt, brev,
        // meldekort og andre steder.
        // Dette gjør at vi kan beholde informasjonen for hver kotlin_module, og kotlin-reflect og andre verktøy
        // fungerer som forventet. Alternativet er å unngå å bruke det eksakt samme navnet på submoduler fra
        // forskjellige moduler, som feks "kontrakt".
        moduleName.set("${rootProject.name}-${project.name}")
    }
}

// Pass på at når vi kaller JavaExec eller Test tasks så bruker vi samme språk-versjon som vi kompilerer til
val toolchainLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
}
tasks.withType<Test>().configureEach { javaLauncher.set(toolchainLauncher) }
tasks.withType<JavaExec>().configureEach { javaLauncher.set(toolchainLauncher) }


kotlin.sourceSets["main"].kotlin.srcDirs("main")
kotlin.sourceSets["test"].kotlin.srcDirs("test")
sourceSets["main"].resources.srcDirs("main")
sourceSets["test"].resources.srcDirs("test")